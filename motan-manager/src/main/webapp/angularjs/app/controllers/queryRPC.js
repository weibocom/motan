'use strict';

app.controller('QueryRPCCtrl', function ($scope, ConfigService, uiGridConstants) {

    var self = this;

    self.getAllGroups = function () {
        ConfigService.getAllGroups().then(
            function (data) {
                $scope.groups = data;
            }
        );
    };

    self.queryRPC = function (group) {
        ConfigService.getAllNodes(group).then(
            function (data) {
                self.queryRPCOptions.data = data;
            }
        );
    };

    self.queryRPCOptions = {
        data: [],
        expandableRowTemplate: 'rpcDetail.html',
        onRegisterApi: function (gridApi) {
            self.gridApi = gridApi;
            self.gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                var server = row.entity.server;
                var unavailableServer = row.entity.unavailableServer;
                var client = row.entity.client;
                var serverLen = Math.max(server.length, 1) * 18 + 17;
                var unavailableLen = Math.max(unavailableServer.length, 1) * 18 + 17;
                var clientLen = Math.max(client.length, 1) * 18 + 17;
                row.expandedRowHeight = serverLen + unavailableLen + clientLen + 1;
            });
        },
        showGridFooter: true,
        enableSorting: true,
        columnDefs: [
            {
                name: 'Service', field: 'service',
                sort: {
                    direction: uiGridConstants.ASC,
                    priority: 1
                }
            },
            {name: 'Server', field: 'server.length'},
            {name: 'UnavailableServer', field: 'unavailableServer.length'},
            {name: 'Client', field: 'client.length'}
        ]
    };

    self.getAllGroups();
});