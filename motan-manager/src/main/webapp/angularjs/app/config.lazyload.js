angular.module('app')
    .config([
        '$ocLazyLoadProvider', function ($ocLazyLoadProvider) {
            $ocLazyLoadProvider.config({
                debug: false,
                events: true,
                modules: [
                    {
                        name: 'toaster',
                        files: [
                            'lib/modules/angularjs-toaster/toaster.css',
                            'lib/modules/angularjs-toaster/toaster.js'
                        ]
                    },
                    {
                        name: 'ui.select',
                        files: [
                            'lib/modules/angular-ui-select/select.css',
                            'lib/modules/angular-ui-select/select.js'
                        ]
                    },
                    {
                        name: 'ui.grid',
                        files: [
                            'lib/modules/angular-ui-grid/ui-grid.min.css',
                            'lib/modules/angular-ui-grid/ui-grid.min.js'
                        ]
                    },
                    {
                        name: 'ui.grid.expandable',
                        files: [
                            'lib/modules/angular-ui-grid/ui-grid.min.css',
                            'lib/modules/angular-ui-grid/ui-grid.js'
                        ]
                    },
                    {
                        name: 'ui.grid.autoResize',
                        files: [
                            'lib/modules/angular-ui-grid/ui-grid.min.css',
                            'lib/modules/angular-ui-grid/ui-grid.js'
                        ]
                    },
                    {
                        name: 'ngTagsInput',
                        files: [
                            'lib/modules/ng-tags-input/ng-tags-input.js'
                        ]
                    },
                    {
                        name: 'daterangepicker',
                        serie: true,
                        files: [
                            'lib/modules/angular-daterangepicker/moment.js',
                            'lib/modules/angular-daterangepicker/daterangepicker.js',
                            'lib/modules/angular-daterangepicker/angular-daterangepicker.js'
                        ]
                    },
                    {
                        name: 'ngGrid',
                        files: [
                            'lib/modules/ng-grid/ng-grid.min.js',
                            'lib/modules/ng-grid/ng-grid.css'
                        ]
                    },
                    {
                        name: 'fuelux.wizard',
                        files: [
                            'lib/modules/angular-ui-fuelux-wizard/wizard.js'
                        ]
                    }
                ]
            });
        }
    ]);