'use strict';

angular.module('app')
    .run(function ($rootScope, $state, $stateParams, $location, $http, $q, $log) {
        $rootScope.$state = $state;
        $rootScope.$stateParams = $stateParams;

        $rootScope.$on('$viewContentLoaded', function () {
            delete $rootScope.error;
        });

        $rootScope.hasRole = function (role) {
            if ($rootScope.user === undefined || $rootScope.user.roles[role] === undefined) {
                return false
            }
            return $rootScope.user.roles[role];
        };

        $rootScope.rememberMe = false;
        $rootScope.originalPath = $location.path();

        var authToken = window.localStorage.getItem('authToken');
        if (authToken != null) {
            window.sessionStorage.setItem('authToken', authToken);
        }
        authToken = window.sessionStorage.getItem('authToken');

        if (authToken != null) {
            $rootScope.authToken = authToken;
            // todo 不太清楚如何在module的run里注入provider，因此直接调用$http
            $http.get('api/user').then(
                function (response) {
                    $rootScope.user = response.data;
                    $location.path($rootScope.originalPath);
                },
                function (errResponse) {
                    $log.error('Error while get user when first visit');
                    return $q.reject(errResponse);
                }
            );
        } else {
            $location.path("/login");
        }

        $rootScope.initialised = true;
    })
    .config(
        [
            '$stateProvider', '$urlRouterProvider', '$httpProvider',
            function ($stateProvider, $urlRouterProvider, $httpProvider) {

                $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
                    return {
                        'responseError': function (rejection) {
                            var status = rejection.status;
                            var config = rejection.config;
                            var method = config.method;
                            var url = config.url;

                            if (status == 401) {
                                $location.path("/login");
                            } else {
                                $rootScope.error = method + " on " + url + " failed with status " + status;
                            }

                            return $q.reject(rejection);
                        }
                    };
                });
                $httpProvider.interceptors.push(function ($q, $rootScope) {
                    return {
                        'request': function (config) {
                            var isRestCall = config.url.indexOf('api') == 0;
                            if (isRestCall && angular.isDefined($rootScope.authToken)) {
                                var authToken = $rootScope.authToken;
                                if (exampleAppConfig.useAuthTokenHeader) {
                                    config.headers['X-Auth-Token'] = authToken;
                                } else {
                                    config.url = config.url + "?token=" + authToken;
                                }
                            }
                            return config || $q.when(config);
                        }
                    }
                });

                $urlRouterProvider
                    .otherwise('/app/queryRPC');
                $stateProvider
                    .state('app', {
                        abstract: true,
                        url: '/app',
                        templateUrl: 'views/layout.html',
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load([]).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/login.js',
                                                        'app/services/user.service.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    })
                    .state('login', {
                        url: '/login',
                        templateUrl: 'views/login.html',
                        ncyBreadcrumb: {
                            label: 'Login'
                        },
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load(['toaster']).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/login.js',
                                                        'app/services/user.service.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    })
                    .state('app.queryRPC', {
                        url: '/queryRPC',
                        templateUrl: 'views/queryRPC.html',
                        ncyBreadcrumb: {
                            label: 'RPC服务查询'
                        },
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load(['ui.select', 'ui.grid', 'ui.grid.expandable', 'ui.grid.autoResize', 'toaster']).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/queryRPC.js',
                                                        'app/services/config.service.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    })
                    .state('app.trafficSwitch', {
                        url: '/trafficSwitch',
                        templateUrl: 'views/trafficSwitch.html',
                        ncyBreadcrumb: {
                            label: '流量切换'
                        },
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load(['ui.select', 'ui.grid', 'ui.grid.expandable', 'ui.grid.autoResize', 'ngTagsInput', 'fuelux.wizard', 'toaster']).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/trafficSwitch.js',
                                                        'app/controllers/modal.js',
                                                        'app/services/config.service.js',
                                                        //'lib/jquery/fuelux/spinbox/fuelux.spinbox.js',
                                                        'lib/jquery/textarea/jquery.autosize.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    })
                    .state('app.queryOrders', {
                        url: '/queryOrders',
                        templateUrl: 'views/queryOrders.html',
                        ncyBreadcrumb: {
                            label: '指令查询'
                        },
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load(['ui.grid', 'ui.grid.expandable', 'ui.grid.autoResize', 'toaster']).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/queryOrders.js',
                                                        'app/controllers/modal.js',
                                                        'app/services/config.service.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    })
                    .state('app.operationRecord', {
                        url: '/operationHistory',
                        templateUrl: 'views/operationRecord.html',
                        ncyBreadcrumb: {
                            label: '操作记录查询'
                        },
                        resolve: {
                            deps: [
                                '$ocLazyLoad',
                                function ($ocLazyLoad) {
                                    return $ocLazyLoad.load(['ui.grid', 'ui.grid.expandable', 'ui.grid.autoResize', 'toaster']).then(
                                        function () {
                                            return $ocLazyLoad.load(
                                                {
                                                    serie: true,
                                                    files: [
                                                        'app/controllers/operationRecord.js',
                                                        'app/services/config.service.js'
                                                    ]
                                                });
                                        }
                                    );
                                }
                            ]
                        }
                    });
            }
        ]
    );