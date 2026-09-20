cmake_minimum_required(VERSION 3.22.1)
project(irremote_ac_protocols LANGUAGES CXX)

add_library(irremote_ac_protocols SHARED
    jni_bridge.cpp
    upstream/IRac.cpp
    upstream/IRsend.cpp
    upstream/IRtimer.cpp
    upstream/IRutils.cpp
    upstream/IRtext.cpp
    upstream/ir_LG.cpp
    upstream/ir_Gree.cpp
    upstream/ir_Panasonic.cpp
    upstream/ir_Daikin.cpp
    upstream/ir_Midea.cpp
    upstream/ir_Kelvinator.cpp
    upstream/ir_Fujitsu.cpp
    upstream/ir_Mitsubishi.cpp
    upstream/ir_Samsung.cpp
    upstream/ir_Tcl.cpp)

target_include_directories(irremote_ac_protocols PRIVATE upstream compat)
target_compile_features(irremote_ac_protocols PRIVATE cxx_std_17)
target_compile_definitions(irremote_ac_protocols PRIVATE
    SWIGLIB
    _IR_ENABLE_DEFAULT_=0
    SEND_LG=1
    SEND_GREE=1
    SEND_PANASONIC_AC=1
    SEND_DAIKIN=1
    SEND_MIDEA=1
    SEND_FUJITSU_AC=1
    SEND_MITSUBISHI_AC=1
    SEND_SAMSUNG_AC=1)
target_compile_options(irremote_ac_protocols PRIVATE -ffunction-sections -fdata-sections -include Arduino.h)
target_link_options(irremote_ac_protocols PRIVATE
    -Wl,--gc-sections
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384)

find_library(android_log log)
find_library(cxx_shared c++_shared REQUIRED)
target_link_libraries(irremote_ac_protocols PRIVATE ${android_log} ${cxx_shared})
