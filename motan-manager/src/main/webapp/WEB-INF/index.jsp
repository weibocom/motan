<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>

<%--
  ~  Copyright 2009-2016 Weibo, Inc.
  ~
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<!DOCTYPE html>

<html xmlns="http://www.w3.org/1999/xhtml" ng-app="app" ng-controller="AppCtrl">

<!-- Head -->
<head>
    <meta charset="utf-8"/>
    <title page-title></title>

    <meta name="description" content="blank page"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="shortcut icon" href="assets/img/favicon.png" type="image/x-icon">

    <!--Basic Styles-->
    <link href="assets/css/bootstrap.min.css" rel="stylesheet"/>
    <link ng-if="settings.rtl" ng-href="assets/css/bootstrap-rtl.min.css" rel="stylesheet"/>
    <link href="assets/css/font-awesome.min.css" rel="stylesheet"/>
    <link href="assets/css/weather-icons.min.css" rel="stylesheet"/>

    <!--Fonts-->
    <style>
        @
        @font-face {
            font-family: 'WYekan';
            src: url('/angularjs/assets/fonts/BYekan.woff') format('woff');
            font-weight: normal;
            font-style: normal;
        }
    </style>
    <!--<link href="http://fonts.googleapis.com/earlyaccess/droidarabickufi.css" rel="stylesheet" type="text/css"/>
    <link href="http://fonts.googleapis.com/css?family=Open+Sans:300italic,400italic,600italic,700italic,400,600,700,300"
          rel="stylesheet" type="text/css">-->

    <!--Beyond styles-->
    <link ng-if="!settings.rtl" href="assets/css/beyond.min.css" rel="stylesheet"/>
    <link href="assets/css/demo.min.css" rel="stylesheet"/>
    <link href="assets/css/typicons.min.css" rel="stylesheet"/>
    <link href="assets/css/animate.min.css" rel="stylesheet"/>
    <link ng-href="{{settings.skin}}" rel="stylesheet" type="text/css"/>

</head>
<!-- /Head -->

<!-- Body -->
<body>
<div ui-view></div>

<!-- Scripts -->
<script src="lib/jquery/jquery.min.js"></script>
<script src="lib/jquery/bootstrap.js"></script>
<script src="lib/angular/angular.js"></script>

<script src="lib/utilities.js"></script>

<script src="lib/angular/angular-animate/angular-animate.js"></script>
<script src="lib/angular/angular-cookies/angular-cookies.js"></script>
<script src="lib/angular/angular-resource/angular-resource.js"></script>
<script src="lib/angular/angular-sanitize/angular-sanitize.js"></script>
<script src="lib/angular/angular-touch/angular-touch.js"></script>

<script src="lib/angular/angular-ui-router/angular-ui-router.js"></script>
<script src="lib/angular/angular-ocLazyLoad/ocLazyLoad.js"></script>
<script src="lib/angular/angular-ngStorage/ngStorage.js"></script>
<script src="lib/angular/angular-ui-utils/angular-ui-utils.js"></script>
<script src="lib/angular/angular-breadcrumb/angular-breadcrumb.js"></script>

<script src="lib/angular/angular-ui-bootstrap/ui-bootstrap.js"></script>
<script src="lib/jquery/slimscroll/jquery.slimscroll.js"></script>


<!-- App Config and Routing Scripts -->
<script src="app/app.js"></script>
<script src="app/config.js"></script>
<script src="app/config.lazyload.js"></script>
<script src="app/config.router.js"></script>
<script src="app/beyond.js"></script>


<!-- Layout Related Directives -->
<script src="app/directives/loading.js"></script>
<script src="app/directives/skin.js"></script>
<script src="app/directives/sidebar.js"></script>
<script src="app/directives/header.js"></script>
<script src="app/directives/navbar.js"></script>
<!--<script src="app/directives/chatbar.js"></script>-->
<script src="app/directives/widget.js"></script>

</body>
<!--  /Body -->
</html>