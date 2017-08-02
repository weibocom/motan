//Chat Toggle Link
angular.module('app')
    .directive('skinChanger', ['$rootScope', '$filter','$state', '$stateParams', function ($rootScope, $filter, $state, $stateParams) {
        return {
            restrict: 'AC',
            link: function (scope, el, attr) {
                el.on('click', function () {
                    setTimeout(function () {
                        $rootScope.$apply(function () {
                            var skincolor = $filter('filter')(scope.skins, function (d) { return d.skin === el.attr('rel'); })[0];
                            $rootScope.settings.skin = skincolor.skin;
                            $rootScope.settings.color = skincolor.color;
                            $state.transitionTo($state.current, $stateParams, {
                                reload: true,
                                inherit: false,
                                notify: true
                            });
                        });
                    }, 100);
                });
                scope.skins = [
                    {
                        skin: 'assets/css/skins/blue.min.css',
                        color: {
                            themeprimary: '#5db2ff',
                            themesecondary: '#ed4e2a',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/azure.min.css',
                        color: {
                            themeprimary: '#2dc3e8',
                            themesecondary: '#fb6e52',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/black.min.css',
                        color: {
                            themeprimary: '#474544',
                            themesecondary: '#d73d32',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/darkblue.min.css',
                        color: {
                            themeprimary: '#0072c6',
                            themesecondary: '#fb6e52',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/darkred.min.css',
                        color: {
                            themeprimary: '#ac193d',
                            themesecondary: '#7bd148',
                            themethirdcolor: '#5db2ff',
                            themefourthcolor: '#e75b8d',
                            themefifthcolor: '#ffce55'
                        }
                    },
                    {
                        skin: 'assets/css/skins/deepblue.min.css',
                        color: {
                            themeprimary: '#001940',
                            themesecondary: '#d73d32',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/deepblue.min.css',
                        color: {
                            themeprimary: '#001940',
                            themesecondary: '#d73d32',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/gray.min.css',
                        color: {
                            themeprimary: '#585858',
                            themesecondary: '#fb6e52',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/green.min.css',
                        color: {
                            themeprimary: '#53a93f',
                            themesecondary: '#ed4e2a',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/orange.min.css',
                        color: {
                            themeprimary: '#ff8f32',
                            themesecondary: '#7bd148',
                            themethirdcolor: '#5db2ff',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/pink.min.css',
                        color: {
                            themeprimary: '#cc324b',
                            themesecondary: '#8cc474',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#e75b8d'
                        }
                    },
                    {
                        skin: 'assets/css/skins/purple.min.css',
                        color: {
                            themeprimary: '#8c0095',
                            themesecondary: '#ffce55',
                            themethirdcolor: '#e75b8d',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#fb6e52'
                        }
                    },
                    {
                        skin: 'assets/css/skins/teal.min.css',
                        color: {
                            themeprimary: '#03b3b2',
                            themesecondary: '#ed4e2a',
                            themethirdcolor: '#ffce55',
                            themefourthcolor: '#a0d468',
                            themefifthcolor: '#fb6e52'
                        }
                    }
                ];
                
            }
        };
    }]);