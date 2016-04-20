'use strict';

app.factory('UserService', function ($http, $log, $q) {
    return {
        getUser: function () {
            return $http.get('api/user').then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while get user');
                    return $q.reject(errResponse);
                }
            );
        },
        authenticate: function (username, password) {
            var data = {'username': username, 'password': password};
            return $http({
                url: 'api/user/authenticate',
                method: 'POST',
                data: $.param(data),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).then(
                function (response) {
                    return response.data;
                },
                function (errResponse) {
                    $log.error('Error while authenticate');
                    return $q.reject(errResponse);
                }
            );
        }
    };
});