<?php declare(strict_types=1); ini_set('error_reporting', '0'); ini_set('display_errors', '0'); $CustomCode$

class MathFloat { //source: https://github.com/OakbankTechnologyInc/Math/blob/master/MathFloat.php
    public static function explode($value) {
        $bin = '';
        $packed = pack('f', $value);
        foreach(str_split(strrev($packed)) as $char)
            $bin .= str_pad(decbin(ord($char)), 8, '0', STR_PAD_LEFT);
        $signBit=bindec(substr($bin,0,1));
        $exponentBits=bindec(substr($bin,1,8));
        $fractionBits=bindec(($exponentBits==0?'0':'1').substr($bin,9));
        return array($signBit,$exponentBits,$fractionBits);
    }
    public static function implode($partsOrSign,$exponent=NULL,$fraction=NULL) {
        if (is_array($partsOrSign)) {
            $sign=$partsOrSign[0];
            $exponent=$partsOrSign[1];
            $fraction=$partsOrSign[2];
        } else
            $sign=$partsOrSign;
        $bits=substr('0'.decbin($sign),-1);
        $bits.=substr('00000000'.decbin($exponent),-8);
        $bits.=substr('00000000000000000000000'.decbin($fraction),-23);
        $str='';
        for ($i=0;$i<4;$i++) $str.=chr(bindec(substr($bits,24-$i*8,8)));
        $temp=unpack("f",$str);
        return $temp[1];
    }
    public static function ulp($f) {
        if (is_nan($f)) return $f;
        if (is_infinite($f)) return INF;
        if ($f == 0.0) return self::implode(0,0,1);
        $bits=self::explode($f);
        $bits[0]=0;
        $mantissa = $bits[2];
        $exponent = $bits[1];
        if ($bits[1] == 0) {
            $bits[2]=1;	//set fraction to smallest possible value
            return self::implode($bits);
        }
        $bits[1] -= 23;
        $bits[2]=0;
        if ($bits[1] == 0) {
            $bits[2] = 1 << -($bits[1] - 1);
            $bits[1] = 0;
        }
        return self::implode($bits);
    }
    public static function equals($a,$b,$epsilon=5) {
        if (!is_array($a)) return (abs($a-$b)<=self::ulp($a));
        foreach ($a as $key=>$dontCare)
            if (abs($a[$key]-$b[$key])>self::ulp($a[$key])*$epsilon) return false;
        return true;
    }
}

$out = fopen('php://output', 'wb');
$TestCases$

fclose($out);
