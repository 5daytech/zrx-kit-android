package com.fridaytech.zrxkit.sign.eip712

import android.annotation.SuppressLint
import android.util.Pair
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.TreeSet
import java.util.regex.Pattern
import org.bouncycastle.util.encoders.Hex
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.AbiTypes
import org.web3j.crypto.Hash.sha3
import org.web3j.crypto.Hash.sha3String
import org.web3j.utils.Numeric

internal class Eip712Encoder {
    private var jsonMessageInString: String = ""
    private var jsonMessageObject: Eip712Data.EIP712Message

    constructor(jsonMessageInString: String) {
        this.jsonMessageInString = jsonMessageInString
        this.jsonMessageObject = parseJSONMessage(jsonMessageInString)
    }

    constructor(message: Eip712Data.EIP712Message) {
        jsonMessageObject = message
    }

    // Matches array declarations like arr[5][10], arr[][], arr[][34][], etc.
    // Doesn't match array declarations where there is a 0 in any dimension.
    // Eg- arr[0][5] is not matched.
    private val arrayTypeRegex = "^([a-zA-Z_$][a-zA-Z_$0-9]*)((\\[([1-9]\\d*)?\\])+)$"
    private val arrayTypePattern = Pattern.compile(arrayTypeRegex)

    // This regex tries to extract the dimensions from the
    // square brackets of an array declaration using the ``Regex Groups``.
    // Eg- It extracts ``5, 6, 7`` from ``[5][6][7]``
    private val arrayDimensionRegex = "\\[([1-9]\\d*)?\\]"
    private val arrayDimensionPattern = Pattern.compile(arrayDimensionRegex)

    // Fields of Entry Objects need to follow a regex pattern
    // Type Regex matches to a valid name or an array declaration.
    private val typeRegex = "^[a-zA-Z_$][a-zA-Z_$0-9]*(\\[([1-9]\\d*)*\\])*$"
    private val typePattern = Pattern.compile(typeRegex)
    // Identifier Regex matches to a valid name, but can't be an array declaration.
    private val identifierRegex = "^[a-zA-Z_$][a-zA-Z_$0-9]*$"
    private val identifierPattern = Pattern.compile(identifierRegex)

    private fun getDependencies(primaryType: String): MutableSet<String> {
        // Find all the dependencies of a type
        val types = jsonMessageObject.types
        val deps = HashSet<String>()

        if (!types.containsKey(primaryType)) {
            return deps
        }

        val remainingTypes = ArrayList<String>()
        remainingTypes.add(primaryType)

        while (remainingTypes.size > 0) {
            val structName = remainingTypes[remainingTypes.size - 1]
            remainingTypes.removeAt(remainingTypes.size - 1)
            deps.add(structName)

            val itr = types[primaryType]!!.iterator()
            while (itr.hasNext()) {
                val entry = itr.next() as Eip712Data.Entry
                if (!types.containsKey(entry.type)) {
                    // Don't expand on non-user defined types
                    continue
                } else if (deps.contains(entry.type)) {
                    // Skip types which are already expanded
                    continue
                } else {
                    // Encountered a user defined type
                    remainingTypes.add(entry.type)
                }
            }
        }

        return deps
    }

    private fun encodeStruct(structName: String): String {
        val types = jsonMessageObject.types

        var structRepresentation = "$structName("
        for (entry in types[structName]!!) {
            structRepresentation += String.format("%s %s,", entry.type, entry.name)
        }
        structRepresentation = structRepresentation.substring(0, structRepresentation.length - 1)
        structRepresentation += ")"

        return structRepresentation
    }

    fun encodeType(primaryType: String): String {
        val deps = getDependencies(primaryType)
        deps.remove(primaryType)

        // Sort the other dependencies based on Alphabetical Order and finally add the primaryType
        val depsAsList = ArrayList(deps)
        depsAsList.sort()
        depsAsList.add(0, primaryType)

        var result = ""
        for (structName in depsAsList) {
            result += encodeStruct(structName)
        }

        return result
    }

    fun hashType(primaryType: String): ByteArray {
        val encoded = encodeType(primaryType)
        return Numeric.hexStringToByteArray(sha3String(encoded))
    }

    private fun getArrayDimensionsFromDeclaration(declaration: String): List<Int> {
        // Get the dimensions which were declared in Schema.
        // If any dimension is empty, then it's value is set to -1.
        val arrayTypeMatcher = arrayTypePattern.matcher(declaration)
        arrayTypeMatcher.find()
        val dimensionsString = arrayTypeMatcher.group(1)
        val dimensionTypeMatcher = arrayDimensionPattern.matcher(dimensionsString)
        val dimensions = ArrayList<Int>()
        while (dimensionTypeMatcher.find()) {
            val currentDimension = dimensionTypeMatcher.group(1)
            if (currentDimension == null) {
                dimensions.add(Integer.parseInt("-1"))
            } else {
                dimensions.add(Integer.parseInt(currentDimension))
            }
        }

        return dimensions
    }

    private fun getDepthsAndDimensions(data: Any?, depth: Int): List<Pair<*, *>> {
        if (data !is List<*>) return ArrayList()

        val list = ArrayList<Pair<*, *>>()
        val dataAsArray = data as List<Any>?
        list.add(Pair(depth, dataAsArray!!.size))
        for (subdimensionalData in dataAsArray) {
            list.addAll(getDepthsAndDimensions(subdimensionalData, depth + 1))
        }

        return list
    }

    @SuppressLint("UseSparseArrays")
    @Throws(Exception::class)
    fun getArrayDimensionsFromData(data: Any?): List<Int> {
        val depthsAndDimensions = getDepthsAndDimensions(data, 0)
        // groupedByDepth has key as depth and value as List(pair(Depth, Dimension))
        val groupedByDepth = depthsAndDimensions.map { it.first to it.second as List<Pair<*, *>> }

        // depthDimensionsMap is aimed to have key as depth and value as List(Dimension)
        val depthDimensionsMap = HashMap<Int, List<Int>>()
        for ((key, value) in groupedByDepth) {
            val pureDimensions = ArrayList<Int>()
            for (depthDimensionPair in value) {
                pureDimensions.add(depthDimensionPair.second as Int)
            }
            depthDimensionsMap[key as Int] = pureDimensions
        }

        val dimensions = ArrayList<Int>()
        for ((key, value) in depthDimensionsMap) {
            val setOfDimensionsInParticularDepth = TreeSet(value)
            if (setOfDimensionsInParticularDepth.size != 1) {
                throw Exception(
                    String.format(
                        "Depth %d of array data has more than one dimensions",
                        key
                    )
                )
            }
            dimensions.add(setOfDimensionsInParticularDepth.first())
        }

        return dimensions
    }

    private fun flattenMultidimensionalArray(data: Any?): List<Any> {
        if (data !is List<*>) {
            return arrayListOf(data!!)
        }

        val flattenedArray = ArrayList<Any>()
        for (arrayItem in (data as List<*>?)!!) {
            for (otherArrayItem in flattenMultidimensionalArray(arrayItem)) {
                flattenedArray.add(otherArrayItem)
            }
        }

        return flattenedArray
    }

    @Throws(Exception::class)
    fun encodeData(primaryType: String, data: HashMap<String, Any>): ByteArray {
        val types = jsonMessageObject.types

        val encTypes = ArrayList<String>()
        val encValues = ArrayList<Any>()

        // Add typehash
        encTypes.add("bytes32")
        encValues.add(hashType(primaryType))

        // Add field contents
        for (field in types[primaryType]!!) {
            val value = data[field.name]

            when {
                field.type == "string" -> {
                    encTypes.add("bytes32")
                    val hashedValue = Numeric.hexStringToByteArray(sha3String((value as String?)!!))
                    encValues.add(hashedValue)
                }

                field.type == "bytes" -> {
                    encTypes.add("bytes32")
                    val hashedValue = sha3((value as ByteArray?)!!)
                    encValues.add(hashedValue)
                }

                types.containsKey(field.type) -> {
                    // User Defined Type
                    val hashedValue = sha3(
                        encodeData(field.type, (value as HashMap<String, Any>?)!!)
                    )
                    encTypes.add("bytes32")
                    encValues.add(hashedValue)
                }

                arrayTypePattern.matcher(field.type).find() -> {
                    val baseTypeName = field.type.substring(0, field.type.indexOf('['))
                    val expectedDimensions = getArrayDimensionsFromDeclaration(
                        field.type
                    )
                    // This function will itself give out errors in case
                    // that the data is not a proper array
                    val dataDimensions = getArrayDimensionsFromData(value)

                    if (expectedDimensions.size != dataDimensions.size) {
                        // Ex: Expected a 3d array, but got only a 2d array
                        throw Exception(
                            String.format(
                                "Array Data %s has dimensions %s, " + "but expected dimensions are %s",
                                value!!.toString(),
                                dataDimensions.toString(),
                                expectedDimensions.toString()
                            )
                        )
                    }
                    for (i in expectedDimensions.indices) {
                        if (expectedDimensions[i] == -1) {
                            // Skip empty or dynamically declared dimensions
                            continue
                        }
                        if (expectedDimensions[i] !== dataDimensions[i]) {
                            throw Exception(
                                String.format(
                                    "Array Data %s has dimensions %s, " + "but expected dimensions are %s",
                                    value!!.toString(),
                                    dataDimensions.toString(),
                                    expectedDimensions.toString()
                                )
                            )
                        }
                    }

                    val arrayItems = flattenMultidimensionalArray(value)
                    val concatenatedArrayEncodingBuffer = ByteArrayOutputStream()
                    for (arrayItem in arrayItems) {
                        val arrayItemEncoding = encodeData(
                            baseTypeName,
                            arrayItem as HashMap<String, Any>
                        )
                        concatenatedArrayEncodingBuffer.write(
                            arrayItemEncoding,
                            0,
                            arrayItemEncoding.size
                        )
                    }
                    val concatenatedArrayEncodings = concatenatedArrayEncodingBuffer.toByteArray()
                    val hashedValue = sha3(concatenatedArrayEncodings)
                    encTypes.add("bytes32")
                    encValues.add(hashedValue)
                }

                else -> {
                    if (value != null) {
                        encTypes.add(field.type)
                        encValues.add(value)
                    }
                }
            }
        }

        return encodePacked(encTypes, encValues)
    }

    private fun encodePacked(types: ArrayList<String>, values: ArrayList<Any>): ByteArray {
        val baos = ByteArrayOutputStream()

        for (i in types.indices) {
            val typeClazz = AbiTypes.getType(types[i]) as Class<Type<*>>

            var atleastOneConstructorExistsForGivenParametersType = false
            // Using the Reflection API to get the types of the parameters
            val constructors = typeClazz.constructors
            for (constructor in constructors) {
                // Check which constructor matches
                try {
                    val parameterTypes = constructor.parameterTypes
                    val temp = Numeric.hexStringToByteArray(
                        TypeEncoder.encode(
                            typeClazz.getDeclaredConstructor(*parameterTypes)
                                .newInstance(values[i])
                        )
                    )
                    baos.write(temp, 0, temp.size)
                    atleastOneConstructorExistsForGivenParametersType = true
                    break
                } catch (e: IllegalArgumentException) {
                    continue
                } catch (e: NoSuchMethodException) {
                    continue
                } catch (e: InstantiationException) {
                    continue
                } catch (e: IllegalAccessException) {
                    continue
                } catch (e: InvocationTargetException) {
                    continue
                }
            }

            if (!atleastOneConstructorExistsForGivenParametersType) {
                throw Exception(
                    String.format(
                        "Received an invalid argument for which no constructor" + " exists for the ABI Class %s",
                        typeClazz.simpleName
                    )
                )
            }
        }

        return baos.toByteArray()
    }

    @Throws(Exception::class)
    fun hashMessage(
        primaryType: String,
        data: HashMap<String, Any>
    ): ByteArray = sha3(encodeData(primaryType, data))

    @Throws(Exception::class)
    fun hashDomain(): ByteArray {
        val data = HashMap<String, Any>()

        data["verifyingContract"] = jsonMessageObject.domain.verifyingContract
        data["name"] = jsonMessageObject.domain.name

        if (jsonMessageObject.domain.chainId > 0) {
            data["chainId"] = jsonMessageObject.domain.chainId
        }

        data["version"] = jsonMessageObject.domain.version
        return sha3(encodeData("EIP712Domain", data))
    }

    @Throws(Exception::class)
    fun validateStructuredData(jsonMessageObject: Eip712Data.EIP712Message) {
        val typesIterator = jsonMessageObject.types.keys.iterator()
        while (typesIterator.hasNext()) {
            val structName = typesIterator.next()
            val fields = jsonMessageObject.types[structName]
            val fieldsIterator = fields!!.iterator()
            while (fieldsIterator.hasNext()) {
                val entry = fieldsIterator.next()
                if (!identifierPattern.matcher(entry.name).find()) {
                    throw Exception(
                        String.format(
                            "Invalid Identifier %s in %s", entry.name, structName
                        )
                    )
                }
                if (!typePattern.matcher(entry.type).find()) {
                    throw Exception(
                        String.format(
                            "Invalid Type %s in %s", entry.type, structName
                        )
                    )
                }
            }
        }
    }

    @Throws(Exception::class)
    fun parseJSONMessage(
        jsonMessageInString: String
    ): Eip712Data.EIP712Message {
        val mapper = ObjectMapper()

        val tempJSONMessageObject = mapper.readValue(
            jsonMessageInString,
            Eip712Data.EIP712Message::class.java
        )
        validateStructuredData(tempJSONMessageObject)

        return tempJSONMessageObject
    }

    @Throws(Exception::class)
    fun hashStructuredData(): ByteArray {
        val baos = ByteArrayOutputStream()

        baos.write(Hex.decode("1901"))

        val domainHash = hashDomain()
        baos.write(domainHash)

        val dataHash = hashMessage(
            jsonMessageObject.primaryType,
            jsonMessageObject.message as HashMap<String, Any>
        )
        baos.write(dataHash)

        return sha3(baos.toByteArray())
    }
}
