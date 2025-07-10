<!DOCTYPE html>
<?php
$demo = isset($_GET["demo"]);
?>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="theme-color" content="#6002ee" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Full Screen Ad</title>

  <link href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css" rel="stylesheet">
  <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
  <script src="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.js"></script>
    <style>
        body {
            margin: 0px;
        }
    </style>

</head>
<body>

<header class="mdc-top-app-bar">
  <div class="mdc-top-app-bar__row">
    <section class="mdc-top-app-bar__section mdc-top-app-bar__section--align-start">
      <span class="mdc-top-app-bar__title">Ad Break</span>
    </section>
    <section class="mdc-top-app-bar__section mdc-top-app-bar__section--align-end" role="toolbar">
    </section>
  </div>
</header>
<main class="mdc-top-app-bar--fixed-adjust">
    <div class="mdc-layout-grid">
    <div class="mdc-layout-grid__inner">
        <div class="mdc-layout-grid__cell mdc-layout-grid__cell--span-6"><img src="./fakead.png">
            <div class="mdc-layout-grid__cell">
                <?php if(!$demo) echo '<button onclick="back_scheme()">Back via Scheme</button>'; ?>
                <?php if($demo) { ?> 
                            <button class="mdc-button mdc-button--raised" onclick="back_scheme()">Continue
                                <div class="mdc-button__ripple"></div>
                            </button>
                <?php } ?>            

                <script>
                    function back_scheme() {
                        window.location.href = "<?= $scheme ?>://scan/<?= cookies2app() ?>";
                    }
                </script>
            </div>
        </div>
    </div>
</main>
