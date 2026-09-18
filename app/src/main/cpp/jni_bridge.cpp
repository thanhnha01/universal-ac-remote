#include <jni.h>

#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

#include "IRac.h"
#include "IRsend.h"
#include "IRremoteESP8266.h"
#include "IRutils.h"

extern std::vector<uint32_t> carrierFrequencyList;

namespace {
constexpr jint kMaximumTimingCount = 4096;
std::mutex g_timing_mutex;  // SWIGLIB timingList is process-global.

void throwJava(JNIEnv* env, const char* type, const char* message) {
  jclass exception = env->FindClass(type);
  if (exception) env->ThrowNew(exception, message);
}

std::string fromJava(JNIEnv* env, jstring value) {
  if (!value) throw std::invalid_argument("Protocol ID is required.");
  const char* chars = env->GetStringUTFChars(value, nullptr);
  if (!chars) throw std::runtime_error("Could not read protocol ID.");
  std::string result(chars);
  env->ReleaseStringUTFChars(value, chars);
  return result;
}

std::string optionalFromJava(JNIEnv* env, jstring value) {
  return value ? fromJava(env, value) : std::string();
}

stdAc::opmode_t toMode(jint mode) {
  switch (mode) {
    case 0: return stdAc::opmode_t::kCool;
    case 1: return stdAc::opmode_t::kHeat;
    case 2: return stdAc::opmode_t::kDry;
    case 3: return stdAc::opmode_t::kFan;
    case 4: return stdAc::opmode_t::kAuto;
    default: throw std::invalid_argument("Unsupported operating mode.");
  }
}

stdAc::fanspeed_t toFan(jint fan) {
  switch (fan) {
    case 0: return stdAc::fanspeed_t::kAuto;
    case 1: return stdAc::fanspeed_t::kMin;
    case 2: return stdAc::fanspeed_t::kMedium;
    case 3: return stdAc::fanspeed_t::kHigh;
    default: throw std::invalid_argument("Unsupported fan setting.");
  }
}

struct ProtocolEntry {
  decode_type_t upstream;
};

ProtocolEntry lookup(const std::string& id) {
  const decode_type_t type = strToDecodeType(id.c_str());
  if (type == decode_type_t::UNKNOWN || !IRac::isProtocolSupported(type))
    throw std::invalid_argument("Unsupported or disabled IRremoteESP8266 A/C protocol: " + id);
  return {type};
}
}  // namespace

extern "C" JNIEXPORT jintArray JNICALL
Java_com_thanhnha_universalacremote_ir_NativeAcEncoder_nativeEncodeAc(
    JNIEnv* env, jobject, jstring protocolId, jstring modelId, jboolean power,
    jint temperature, jint mode, jint fan, jboolean swingVertical,
    jboolean swingHorizontal) {
  try {
    const ProtocolEntry protocol = lookup(fromJava(env, protocolId));
    const std::string modelName = optionalFromJava(env, modelId);
    const int16_t model = modelName.empty() ? -1 : IRac::strToModel(modelName.c_str(), -1);
    if (!modelName.empty() && model < 0)
      throw std::invalid_argument("Unknown upstream model/variant ID: " + modelName);
    std::lock_guard<std::mutex> lock(g_timing_mutex);
    IRac encoder(0);
    encoder.resetTiming();
    const bool accepted = encoder.sendAc(
        protocol.upstream, model,
        power == JNI_TRUE, toMode(mode), static_cast<float>(temperature), true,
        toFan(fan), swingVertical == JNI_TRUE ? stdAc::swingv_t::kAuto : stdAc::swingv_t::kOff,
        swingHorizontal == JNI_TRUE ? stdAc::swingh_t::kAuto : stdAc::swingh_t::kOff,
        false, false, false, false, false, false, false, -1, -1);
    if (!accepted) throw std::invalid_argument("IRremoteESP8266 does not support this state for the selected protocol.");

    const std::vector<int> timings = encoder.getTiming();
    const std::vector<uint32_t> carrierFrequencies = carrierFrequencyList;
    if (carrierFrequencies.empty() || carrierFrequencies.front() == 0)
      throw std::runtime_error("IRremoteESP8266 did not report a carrier frequency.");
    for (const uint32_t frequency : carrierFrequencies)
      if (frequency != carrierFrequencies.front())
        throw std::invalid_argument("This transmission uses multiple carrier frequencies and needs special handling.");
    if (timings.empty() || timings.size() > kMaximumTimingCount)
      throw std::runtime_error("IRremoteESP8266 returned an invalid timing sequence.");
    std::vector<jint> result;
    result.reserve(timings.size() + 1);
    result.push_back(static_cast<jint>(carrierFrequencies.front()));
    for (const int timing : timings) {
      if (timing <= 0) throw std::runtime_error("IRremoteESP8266 returned a non-positive timing.");
      result.push_back(static_cast<jint>(timing));
    }
    jintArray output = env->NewIntArray(static_cast<jsize>(result.size()));
    if (!output) return nullptr;
    env->SetIntArrayRegion(output, 0, static_cast<jsize>(result.size()), result.data());
    return output;
  } catch (const std::invalid_argument& error) {
    throwJava(env, "java/lang/IllegalArgumentException", error.what());
  } catch (const std::exception& error) {
    throwJava(env, "java/lang/IllegalStateException", error.what());
  } catch (...) {
    throwJava(env, "java/lang/IllegalStateException", "Unknown native waveform generation error.");
  }
  return nullptr;
}
