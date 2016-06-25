package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.VBNetExecutor;

public class VBNetExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("Public Function helloWorld() As String").append(CodeExecutorBase.NEWLINE).append("\tReturn \"Hello, World!\"").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function concatStrings(a As String, b As String) As String").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function minChar(a As Char, b As Char) As Char").append(CodeExecutorBase.NEWLINE).append("\tReturn If(a < b, a, b)").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function exor(a As Boolean, b As Boolean) As Boolean").append(CodeExecutorBase.NEWLINE).append("\tReturn a Xor b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt8(a As SByte, b As SByte) As SByte").append(CodeExecutorBase.NEWLINE).append("\tReturn CSByte(a + b)").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt16(a As Short, b As Short) As Short").append(CodeExecutorBase.NEWLINE).append("\tReturn CShort(a + b)").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt32(a As Integer, b As Integer) As Integer").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt64(a As Long, b As Long) As Long").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumFloat32(a As Single, b As Single) As Single").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumFloat64(a As Double, b As Double) As Double").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumDecimal(a As Decimal, b As Decimal) As Decimal").append(CodeExecutorBase.NEWLINE).append("\tReturn a + b").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt32Array(a As Integer(), l As Integer) As Integer").append(CodeExecutorBase.NEWLINE).append("\tReturn a.Sum()").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt32List(l As List(Of Integer)) As Integer").append(CodeExecutorBase.NEWLINE).append("\tReturn l.Sum()").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function sumInt32Set(s As HashSet(Of Integer)) As Integer").append(CodeExecutorBase.NEWLINE).append("\tReturn s.Sum()").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function getMapEntry(d As Dictionary(Of Integer, String), k As Integer) As String").append(CodeExecutorBase.NEWLINE).append("\tReturn d(k)").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function getMapListEntry(d As Dictionary(Of Integer, List(Of String)), k As Integer, i As Integer) As String").append(CodeExecutorBase.NEWLINE).append("\tReturn d(k)(i)").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function infiniteLoop() As Integer").append(CodeExecutorBase.NEWLINE).append("\tWhile(True)").append(CodeExecutorBase.NEWLINE).append("\tEnd While").append(CodeExecutorBase.NEWLINE).append("\tReturn 0").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function recursion() As Integer").append(CodeExecutorBase.NEWLINE).append("\tReturn recursion()").append(CodeExecutorBase.NEWLINE).append("End Function").append(CodeExecutorBase.NEWLINE)
																															.append("Public Function [error]() As Integer").append(CodeExecutorBase.NEWLINE).append("\tThrow New Exception()").append(CodeExecutorBase.NEWLINE).append("End Function").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new VBNetExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return VBNetExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return VBNetExecutorTest.FRAGMENT;
	}
}
