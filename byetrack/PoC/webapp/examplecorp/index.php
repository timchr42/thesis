<style>
    body { 
        font-size: 50px;
    }
    </style>

<?php

//ini_set('display_errors', '1');
//ini_set('display_startup_errors', '1');
//error_reporting(E_ALL);

require_once("util.php");
$demo = isset($_GET["demo"]);

if(!$demo) echo "<h1>Examplecorp.de (<code>ad.com</code>)</h1>";

?>




<!--
This website is part of a research project.
DO NOT USE IT! LEAVE NOW!
-->

<?php


//print_r($_COOKIE);
$file = "txt.log";
$cookieName = "crossapptracking";
$apps = array();

//echo "<br><code>";
//echo "Referer: " . $_SERVER['HTTP_REFERER'] . "<br>";
//echo "USER-AGENT [PHP]: ".$_SERVER['HTTP_USER_AGENT']."<br>";
//echo "USER-AGENT [JS] : <script>document.write(navigator.userAgent);</script> <br></code>";

if (isset($_GET['app'])) {
    //echo "<p>App Detected: <b>" . sanitize($_GET['app']) . "</b></p><br>";
    $apps[$_GET['app']] = "yes";
    //file_put_contents($file, "request contained app " . $_GET['app'] . "\n", FILE_APPEND);
}
if (isset($_COOKIE[$cookieName])) {
    $knownApps = explode(";", $_COOKIE[$cookieName]);
    //file_put_contents($file, "cookie detected" . "\n", FILE_APPEND);
    //echo "<p>cookie detected<p><br>";
    //echo cookies2app()."<br>";
    foreach ($knownApps as $app) {
        //file_put_contents($file, "cookie contained app " . $app . "\n", FILE_APPEND);
        if ($app != "") {
            $apps[$app] = "yes";
        }
    }
} 
if (isset($_GET["c"])){
    // for the sake of this PoC we just overwrite the current store.
    // for a real deployment one would implement a merge algo
    $apps = array();
    $knownApps = explode(";", app2cookies($_GET["c"])["crossapptracking"]);
    foreach ($knownApps as $app) {
        if ($app != "") {
            $apps[$app] = "yes";
        }
    }
}
if(count($apps) == 0){
    $apps["ID:".rand(99999,99999999)] = "yes";
}

if(isset($_GET["redirect"])){
    $target = "https://www.schnellnochraviolimachen.de";
    echo '<script>
        setTimeout(function(){
            window.location.href = "' . $target . '";
        }, 1000);
    </script>';
    echo "Redirecting via JS in 3 seconds...";
}
?>
<?php 
if(!$demo) {
?>
<p>Data:</p>
<ul>
    <?php
    foreach ($apps as $app => $wayne) {
        echo "<li>" . sanitize($app) . "</li>";
    }
    ?>
</ul>
<?php
}
?>

<?php
$cookieString = "";
foreach ($apps as $app => $wayne) {
    $cookieString = $cookieString . ";" . $app;
}

setcookie($cookieName, $cookieString); // ["samesite" => "None", "secure" => true]);
$_COOKIE[$cookieName] = $cookieString; // update our internal representation of the client's cookies
//file_put_contents($file, "------------------------\n", FILE_APPEND);

if(isset($_GET["hide"])) {
    require_once("hide.php");
}

?>
