'use strict';

app.controller('OperationRecordCtrl', function ($scope, $rootScope, $modal, ConfigService, uiGridConstants) {
    var self = this;

    var formatData = function (row, index) {
        row.status = row.status === 1 ? 'success' : 'failed';
        var date = new Date(row.createTime);
        row.createTime = date.toLocaleString();
    };

    self.getAllRecord = function () {
        ConfigService.getAllRecord().then(
            function (data) {
                data.forEach(formatData);
                self.operationRecordOptions.data = data;
            }
        );
    };

    self.operationRecordOptions = {
        data: [],
        expandableRowTemplate: 'operationRecord.html',
        onRegisterApi: function (gridApi) {
            self.gridApi = gridApi;
            self.gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                //$rootScope.orders = row.entity.command.clientCommandList;
                //var length = 35;
                //for (var i = 0; i < $rootScope.orders.length; i++) {
                //    var len = $rootScope.orders[i].mergeGroups.length * 18 + 17;
                //    length += 35 * 3 + len + 17;
                //}
                row.expandedRowHeight = 30;
            });
            self.gridApi.grid.registerRowsProcessor(self.singleFilter, 200);
        },
        showGridFooter: true,
        enableSorting: true,
        columnDefs: [
            {
                name: '操作人',
                field: 'operator'
            },
            {
                name: '操作类型',
                field: 'type'
            },
            {
                name: 'Group组名',
                field: 'groupName'
            },
            {
                name: '操作状态',
                field: 'status'
            },
            {
                name: '操作时间',
                field: 'createTime',
                type: 'date'
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
            ['operator', 'type', 'groupName', 'status', 'createTime'].forEach(function (field) {
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

    self.getAllRecord();
});