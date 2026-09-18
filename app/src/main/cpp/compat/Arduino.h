#pragma once

#include <cstdint>
#include <string>
#include <utility>

class String : public std::string {
 public:
  using std::string::string;
  using std::string::operator=;
  String() = default;
  String(const std::string& value) : std::string(value) {}
  String(std::string&& value) : std::string(std::move(value)) {}

  String substring(unsigned int start) const {
    return String(std::string::substr(start));
  }
  String substring(unsigned int start, unsigned int end) const {
    return String(std::string::substr(start, end - start));
  }
};

constexpr uint8_t OUTPUT = 1;
constexpr uint8_t LOW = 0;
constexpr uint8_t HIGH = 1;
inline void pinMode(uint16_t, uint8_t) {}
inline void digitalWrite(uint16_t, uint8_t) {}
inline void delayMicroseconds(uint32_t) {}
inline void delay(uint32_t) {}
inline uint32_t micros() { return 0; }
inline uint32_t millis() { return 0; }

#define F(value) value
