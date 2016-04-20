'use strict';

app.controller('WizardCtrl', function ($scope, $rootScope, $state, $modal, $log, toaster) {
    $rootScope.selectedGroup;   // 来源流量
    $rootScope.groups = [];     // group分组列表
    $rootScope.selectedService; // Service接口类名
    $rootScope.targetGroup = {};
    $rootScope.targetGroups = [];   // 目标流量列表
    $rootScope.services = [];
    $rootScope.routes = [];     // 路由规则列表
    $rootScope.previewIP;       // 预览IP
    $rootScope.clientCommand;
    $rootScope.index = 0;       // 指令序号

    var self = this;
    $scope.stepCurrent = 0;

    $rootScope.verifyModal = function (size) {
        var modalInstance = $modal.open({
            templateUrl: 'verifyModal.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                $rootScope.submit();
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };
    $rootScope.successModal = function (size) {
        var modalInstance = $modal.open({
            templateUrl: 'successModal.html',
            controller: 'ModalInstanceCtrl',
            size: size,
            resolve: {}
        });

        modalInstance.result.then(
            function () {
                $state.go('app.queryOrders');
            }, function () {
                $log.info("Modal dismissed at: " + new Date());
            })
    };
    $rootScope.failedModal = function (size) {
        var modalInstance = $modal.open({
            templateUrl: 'failedModal.html',
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

    self.validate = function (event) {
        // return true to prevent next
        //console.log(event);
        if (event.fromStep == 0) {
            if ($rootScope.selectedGroup === undefined) {
                toaster.pop('warning', 'Warning :', '请选择RPC分组');
                //toaster.error("title", "text");
                return true;
            }
            $rootScope.targetGroups = [$rootScope.selectedGroup];
        }
        if (event.fromStep == 1 && event.toStep == 2) {
            for (var i = 0; i < $rootScope.targetGroups.length; i++) {
                var target = $rootScope.targetGroups[i];
                if (target.weight === undefined || target.weight < 0 || target.weight > 100) {
                    toaster.pop('warning', 'Warning :', '权重比例应在[1,100]之间');
                    return true;
                }
            }
        }
        if (event.fromStep == 2 && event.toStep == 3) {
            $rootScope.preview();
        }
    };

    self.verify = function (event) {
        // return true to prevent next
        $rootScope.verifyModal();
    };
});

app.controller('TrafficSwitchCtrl', function ($scope, $rootScope, ConfigService) {
    var self = this;

    self.getAllGroups = function () {
        ConfigService.getAllGroups().then(
            function (data) {
                for (var i in data) {
                    var group = {};
                    group.name = data[i];
                    group.weight = 1;
                    $rootScope.groups[i] = group;
                }
            }
        );
    };
    self.getServicesByGroup = function (group) {
        ConfigService.getServicesByGroup(group.name).then(
            function (data) {
                for (var i in data) {
                    var service = data[i];
                    if (service.indexOf('/') > 0) {
                        data[i] = service.substring(0, service.indexOf('/'));
                    }
                }
                $rootScope.selectedGroup = group;
                $rootScope.selectedService = '*';
                $rootScope.services = angular.copy(data);
            }
        );
    };
    self.getServiceNodes = function (group, service) {
        ConfigService.getServiceNodes(group, service).then(

        );
    };
    self.addRoute = function () {
        var from = self.routeFrom;
        var to = self.routeTo;
        if (from == undefined || to == undefined)
            return;
        var obj = {text: from + " to " + to};
        if (!containsObject(obj, $rootScope.routes))
            $rootScope.routes.push(obj);
    };
    function containsObject(obj, list) {
        for (var i = 0; i < list.length; i++) {
            if (list[i].text == obj.text) {
                return true;
            }
        }
        return false;
    }

    self.getAllGroups();
});

app.controller('PreviewCtrl', function ($scope, $rootScope, ConfigService, CommandService, uiGridConstants) {
    var self = this;

    self.previewOptions = {
        data: [],
        expandableRowTemplate: 'previewDetail.html',
        onRegisterApi: function (gridApi) {
            self.gridApi = gridApi;
            self.gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                var data = row.entity.urls;
                var length = 35 + data.length * 35;
                row.expandedRowHeight = length + 1;
            });
            self.gridApi.grid.registerRowsProcessor(self.singleFilter, 200);
        },
        showGridFooter: true,
        enableSorting: true,
        columnDefs: [
            {
                name: 'Service',
                field: 'cluster',
                sort: {
                    direction: uiGridConstants.ASC,
                    priority: 1
                }
            },
            {
                name: 'Working',
                field: 'urls.length',
                sort: {
                    direction: uiGridConstants.ASC,
                    priority: 0
                },
                suppressRemoveSort: true,
                cellClass: function (grid, row, col, rowRenderIndex, colRenderIndex) {
                    if (grid.getCellValue(row, col) == '0') {
                        return 'red';
                    }
                }
            }
        ]
    };
    self.buildTargetGroup = function (groups) {
        var targetGroup = [];
        for (var i in groups) {
            targetGroup.push(groups[i].name + ":" + groups[i].weight);
        }
        return targetGroup;
    };
    self.buildRouteRules = function (rules) {
        var routeRules = [];
        for (var i in rules) {
            routeRules.push(rules[i].text);
        }
        return routeRules;
    };

    $rootScope.preview = function () {
        var group = $rootScope.selectedGroup.name;
        var clientCommand = CommandService.buildClientCommand($rootScope.index, $rootScope.selectedService,
            self.buildTargetGroup($rootScope.targetGroups), self.buildRouteRules($rootScope.routes), $rootScope.previewIP);
        $rootScope.clientCommand = clientCommand;
        ConfigService.previewCommand(group, clientCommand).then(
            function (data) {
                self.previewOptions.data = data;
            }
        );
    };

    $rootScope.submit = function () {
        var group = $rootScope.selectedGroup.name;
        var clientCommand = CommandService.buildClientCommand($rootScope.index, $rootScope.selectedService,
            self.buildTargetGroup($rootScope.targetGroups), self.buildRouteRules($rootScope.routes));
        ConfigService.addCommand(group, clientCommand).then(
            function (data) {
                $rootScope.successModal();
            },
            function (err) {
                $rootScope.failedModal();
            }
        );
    };

    self.filter = function () {
        self.gridApi.grid.refresh();
    };

    self.singleFilter = function (renderableRows) {
        var matcher = new RegExp(self.filterValue, 'i');
        renderableRows.forEach(function (row) {
            var match = false;
            ['cluster'].forEach(function (field) {
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
});