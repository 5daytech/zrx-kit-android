[![Jitpack](https://jitpack.io/v/blocksdecoded/zrx-kit-android.svg)](https://jitpack.io/#blocksdecoded/zrx-kit-android)
[![CircleCI](https://circleci.com/gh/blocksdecoded/zrx-kit-android/tree/master.svg?style=shield)](https://circleci.com/gh/blocksdecoded/zrx-kit-android/tree/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen.svg)](https://opensource.org/licenses/MIT)
# ZrxKit
0x Exchange protocol implementation in Kotlin.

## Features
* 0x SRA interaction
* Fill/Cancel/Create 0x orders
* EIP712 typed data encoding
* Wrap/Unwrap Ethereum
* Encoding/Decoding of ABIv2 struct types

## Prerequisites
* JDK >= 1.8
* Android 6 (minSdkVersion 23) or greater

## Installation
Add the JitPack to module build.gradle:
```
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
```
Add the following dependency to your build.gradle file:
```
dependencies {
    implementation 'com.github.blocksdecoded:zrx-kit-android:master-SNAPSHOT'
}
```

## License
    MIT License

    Copyright (c) 2019
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
    