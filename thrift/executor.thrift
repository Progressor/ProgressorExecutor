
# https://thrift.apache.org/docs/idl

# .\thrift\thrift-0.9.3.exe -gen java .\thrift\executor.thrift
# .\thrift\thrift-0.9.3.exe -gen js:node .\thrift\executor.thrift

namespace java ch.bfh.progressor.executor

const string TypeContainerArray = "array"; #e.g. array<string>
const string TypeContainerList = "list"; #e.g. list<string>
const string TypeContainerSet = "set"; #e.g. set<string>
const string TypeContainerMap = "map"; #e.g. map<string, string>

const string TypeString = "string";
const string TypeCharacter = "char";
const string TypeBoolean = "bool";
const string TypeByte = "byte";
const string TypeShort = "short";
const string TypeInteger = "int";
const string TypeLong = "long";
const string TypeSingle = "single";
const string TypeDouble = "double";
const string TypeDecimal = "decimal";

struct TestCase {
	1: string functionName,
	2: list<string> inputTypes,
	3: list<string> inputValues,
	4: list<string> outputTypes,
	5: list<string> expectedOutputValues
}

struct Result {
	1: bool success,
	2: string result,
	3: PerformanceIndicators performance
}

struct PerformanceIndicators {
	1: i64 runtimeMilliSeconds
}

service ExecutorService {

	list<Result> execute(
		1: string language,
		2: string fragment,
		3: list<TestCase> testCases
	)
}
