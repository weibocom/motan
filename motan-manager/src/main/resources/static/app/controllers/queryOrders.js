'use strict';

app.controller('QueryOrdersCtrl', function ($scope, $rootScope, $log, ConfigService, uiGridConstants) {

    var self = this;
    $rootScope.orders = [];
    $rootScope.commands = [];

    self.queryOrders = function () {
        ConfigService.getAllCommands().then(
            function (data) {
                $rootScope.commands = data;
                self.queryOrdersOptions.data = data;
            }
        );
    };

    self.orderReset = function () {
        $rootScope.order = {
            index: null,
            version: "",
            dc: "",
            pattern: "",
            mergeGroups: [],
            routeRules: [],
            remark: ""
        };
        //$scope.myForm.$setPristine(); //reset Form
    };

    self.queryOrdersOptions = {
        data: [],
        expandableRowTemplate: 'orderDetail.html',
        onRegisterApi: function (gridApi) {
            self.gridApi = gridApi;
            self.gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                // calculate row height
                $rootScope.orders = row.entity.command.clientCommandList;
                if ($rootScope.orders.routeRules === undefined) {
                    $rootScope.orders.routeRules = [];
                }
                var length = 35;
                for (var i = 0; i < $rootScope.orders.length; i++) {
                    var len = $rootScope.orders[i].mergeGroups.length * 18 + 17;
                    length += 35 * 3 + len + 17;
                }
                row.expandedRowHeight = length + 1;
            });
            self.gridApi.grid.registerRowsProcessor(self.singleFilter, 200);
        },
        showGridFooter: true,
        enableSorting: true,
        columnDefs: [
            {
                name: 'Group',
                field: 'group',
                sort: {
                    direction: uiGridConstants.ASC,
                    priority: 1
                }
            },
            {
                name: '指令条数',
                field: 'command.clientCommandList.length'
            }
        ],
        enableHorizontalScrollbar: uiGridConstants.scrollbars.NEVER
    };

    self.filter = function () {
        self.gridApi.grid.refresh();
    };

    self.singleFilter = function (renderableRows) {
        var matcher = new RegExp(self.filterValue, 'i');
        renderableRows.forEach(function (row) {
            var match = false;
            ['group'].forEach(function (field) {
                if (row.entity[field].match(matcher)) {
                    match = true;
                }
            });
            if (!match) {
                row.visible = false;
            }
        });
        return renderableRows;
    };

    self.queryOrders();
});

app.controller('ModalCtrl', function ($scope, $rootScope, $state, $modal, $log, ConfigService) {
    var self = this;

    self.updateModal = function (group, order, size) {
        $rootScope.order = angular.copy(order);
        if ($rootScope.order.routeRules.length == 0) {
            $rootScope.order.routeRules = [''];
        }
        $rootScope.group = group;

        var modalInstance = $modal.open({
            templateUrl: 'updateModal.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                self.updateOrder(group, $rootScope.order);
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };

    self.updateOrder = function (group, order) {
        ConfigService.updateCommand(group, order).then(
            function (data) {
                self.operationSuccess();
            },
            function (err) {
                self.operationFailed();
            }
        );
    };

    self.deleteModal = function (group, order, size) {
        $rootScope.order = angular.copy(order);

        var modalInstance = $modal.open({
            templateUrl: 'deleteModal.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                self.deleteOrder(group, order.index);
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };

    self.deleteOrder = function (group, index) {
        ConfigService.deleteCommand(group, index).then(
            function (data) {
                self.operationSuccess();
            },
            function (err) {
                self.operationFailed();
            }
        );
    };

    self.operationSuccess = function (size) {
        var modalInstance = $modal.open({
            templateUrl: 'operationSuccess.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                $state.reload();
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };

    self.operationFailed = function (size) {
        var modalInstance = $modal.open({
            templateUrl: 'operationFailed.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                $log.info("Null");
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };
});