'use strict';

app.factory('ConfigService', function ($http, $log, $q) {
    return {
        getAllGroups: function () {
            return $http.get('api/groups').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while fetching groups');
                    return $q.reject(errResponse);
                }
            );
        },
        getServicesByGroup: function (group) {
            return $http.get('api/' + group + '/services').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while fetching services of' + group);
                    return $q.reject(errResponse);
                }
            );
        },
        getServiceNodes: function (group, service) {
            return $http.get('api/' + group + '/' + service + '/service/nodes').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while fetching Nodes info of ' + group + ' ' + service);
                    return $q.reject(errResponse);
                }
            );
        },
        getAllNodes: function (group) {
            return $http.get('api/' + group + '/nodes').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while fetching nodes ' + group);
                    return $q.reject(errResponse);
                }
            );
        },
        getAllCommands: function () {
            return $http.get('api/commands').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while fetching commands');
                    return $q.reject(errResponse);
                }
            );
        },
        addCommand: function (group, clientCommand) {
            return $http.post('api/commands/' + group, clientCommand).then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    if (errResponse.status === 403) {
                        $log.error('No permission while add command');
                    } else {
                        $log.error('Error while add command ' + group);
                    }
                    return $q.reject(errResponse);
                }
            );
        },
        updateCommand: function (group, clientCommand) {
            return $http.put('api/commands/' + group, clientCommand).then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    if (errResponse.status === 403) {
                        $log.error('No permission while update command');
                    } else {
                        $log.error('Error while update command ' + group);
                    }
                    return $q.reject(errResponse);
                }
            );
        },
        deleteCommand: function (group, index) {
            return $http.delete('api/commands/' + group + '/' + index).then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    if (errResponse.status === 403) {
                        $log.error('No permission while delete command');
                    } else {
                        $log.error('Error while delete command ' + group + ' ' + index);
                    }
                    return $q.reject(errResponse);
                }
            );
        },
        previewCommand: function (group, clientCommand) {
            return $http.post('api/commands/' + group + '/preview', clientCommand).then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    if (errResponse.status === 403) {
                        $log.error('No permission while preview command');
                    } else {
                        $log.error('Error while preview command ' + group);
                    }
                    return $q.reject(errResponse);
                }
            );
        },
        getAllRecord: function () {
            return $http.get('api/commands/operationRecord').then(
                function (response) {
                    if (response.status == 204) {
                        $log.warn('No database found');
                        return [];
                    } else {
                        return response.data;
                    }
                },
                function (errResponse) {
                    $log.error('Error while fetching operationRecord');
                    return $q.reject(errResponse);
                }
            );
        }
    };
});

app.factory('CommandService', function () {
    return {
        buildClientCommand: function (index, service, targetGroups, routeRules, previewIP) {
            return {
                index: index,
                mergeGroups: targetGroups,
                pattern: service,
                routeRules: routeRules,
                previewIP: previewIP
            };
        }
    };
});