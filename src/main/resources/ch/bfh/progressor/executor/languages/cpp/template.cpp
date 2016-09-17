#include <cstdlib>
#include <cmath>
#include <iostream>
#include <string>
#include <regex>
#include <algorithm>
#include <iterator>
#include <array>
#include <list>
#include <vector>
#include <set>
#include <map>

using namespace std;

template<class T>
bool hasMinimalDifference(T value1, T value2);

template<class T>
bool hasSameElements(T *array1, T *array2, int length);

#line 1
$CustomCode$

#include <chrono>

using namespace std::chrono;

int main() {
	$TestCases$

	return EXIT_SUCCESS;
}

template<class T>
bool hasMinimalDifference(T value1, T value2) {

	union {
		T d64;
		int64_t i64;
	} u { value1 };
	u.i64++;

	if (!isnormal(value1) || !isnormal(value2)) return value1 == value2;
	if (value1 == value2) return true;
	return abs(value1 - value2) <= u.d64 - value1;
}

template<class T>
bool hasSameElements(T *array1, T *array2, int length) {
	return equal(array1, array1 + length, array2);
}
