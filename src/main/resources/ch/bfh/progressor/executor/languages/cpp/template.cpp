#include <cstdlib>
#include <cmath>
#include <iostream>
#include <string>
#include <sstream>
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

template<class T>
string printArray(T a, int32_t l);

template<class T>
string printCollection(T c);

template<class T>
string printMap(T m);

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

template<class T>
string printArray(T a, int32_t l) {

	stringstream r;
	r << "{ ";
	for (int32_t i = 0; i < l; i++) {
		if (i != 0)
			r << ", ";
		r << a[i];
	}
	r << " }";
	return r.str();
}

template<class T>
string printCollection(T c) {

	stringstream r;
	r << "{ ";
	for (typename T::iterator i = c.begin(); i != c.end(); i++) {
		if (i != c.begin())
			r << ", ";
		r << *i;
	}
	r << " }";
	return r.str();
}

template<class T>
string printMap(T m) {

	stringstream r;
	r << "{ ";
	for (typename T::iterator i = m.begin(); i != m.end(); i++) {
		if (i != m.begin())
			r << ", ";
		r << i->first << ": " << i->second;
	}
	r << " }";
	return r.str();
}
