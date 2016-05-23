from decimal import *
$CustomCode$

### source: http://stackoverflow.com/a/3064143/1325979 ###
import struct, functools, math
def c_mem_cast(x, f=None, t=None):
	return struct.unpack(t, struct.pack(f, x))[0]

dbl_to_lng = functools.partial(c_mem_cast, f='d', t='q')

def ulp_diff_maker(converter, negative_zero):
	def the_diff(a, b):
		ai = converter(a)
		if ai < 0: ai = negative_zero - ai
		bi = converter(b)
		if bi < 0: bi = negative_zero - bi
		return abs(ai - bi)
	return the_diff

dulpdiff = ulp_diff_maker(dbl_to_lng, 0x8000000000000000)

def hasMinimalDifference(value1, value2):
	if (not math.isfinite(value1) or not math.isfinite(value2)):
		return value1 == value2
	if (value1 == value2):
		return True
	return dulpdiff(value1, value2) <= 1

import sys, codecs, time
sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach()) #source: http://stackoverflow.com/a/4374457/1325979
$TestCases$
