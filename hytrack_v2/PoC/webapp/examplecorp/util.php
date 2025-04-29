<?php
$key = "b2rHk)2Rm4Q54jb)NQDdcaycro)]c";

$scheme = $_GET["s"];


// https://www.php.net/manual/en/function.base64-encode.php#103849
function base64url_encode($data)
{

    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}



function base64url_decode($data)
{

    return base64_decode(str_pad(strtr($data, '-_', '+/'), strlen($data) % 4, '=', STR_PAD_RIGHT));
}

function sanitize(string $data): string
{
    return htmlspecialchars($data, ENT_QUOTES, 'UTF-8');
}

function cookies2app(): string
{
    //echo var_dump($_COOKIE);
    $serialized = serialize($_COOKIE);
    $sha256 = hash('sha256', $serialized);

    return base64url_encode(encrypt($sha256 . $serialized));
}

function encrypt(string $input)
{
    global $key;
    // this is a simple demo function
    // for a real deployment one would use a safe cipher with integrity checks
    // lets just use XOR for demo purposes...
    $out = "";
    for ($i = 0; $i < strlen($input); $i++) {
        $out .= $input[$i] ^ $key[$i % strlen($key)];
    }
    return $out;
}

function app2cookies($str)
{
    $str = encrypt(base64url_decode($str));
    $hash = substr($str, 0, 64);
    $payload = substr($str, 64);
    if ($hash !== hash("sha256", $payload)) {
        echo "<h1> HASH FAILED </h1>";
        echo $hash;
        return;
    }
    return unserialize($payload);
}
