Imports System : Imports System.Collections.Generic : Imports System.Linq : Imports System.Text.RegularExpressions : Class Program
$CustomCode$

	Shared Sub Main(ParamArray args As String())

		Console.OutputEncoding = System.Text.Encoding.GetEncoding("UTF-8")
		Dim inst As New Program()
$TestCases$
	End Sub

	Public Shared Function HasMinimalDifference(value1 As Single, value2 As Single, units As Integer) As Boolean

		If Single.IsNaN(value1) OrElse Single.IsInfinity(value1) OrElse Single.IsNaN(value2) OrElse Single.IsInfinity(value2) Then Return value1 = value2
		If value1 = value2 Then Return True

		Dim iValue1 As Integer = BitConverter.ToInt32(BitConverter.GetBytes(value1), 0)
		Dim iValue2 As Integer = BitConverter.ToInt32(BitConverter.GetBytes(value2), 0)

		If (iValue1 >> 31) <> (iValue2 >> 31) Then Return value1 = value2
		Return Math.Abs(iValue1 - iValue2) <= units
	End Function

	Public Shared Function HasMinimalDifference(value1 As Double, value2 As Double, units As Integer) As Boolean

		If Double.IsNaN(value1) OrElse Double.IsInfinity(value1) OrElse Double.IsNaN(value2) OrElse Double.IsInfinity(value2) Then Return value1 = value2
		If value1 = value2 Then Return True

		Dim lValue1 As Long = BitConverter.DoubleToInt64Bits(value1)
		Dim lValue2 As Long = BitConverter.DoubleToInt64Bits(value2)

		If (lValue1 >> 63) <> (lValue2 >> 63) Then Return value1 = value2
		Return Math.Abs(lValue1 - lValue2) <= units
	End Function
End Class
