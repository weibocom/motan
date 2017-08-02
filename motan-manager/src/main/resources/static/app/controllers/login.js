'use strict';

app.controller('LoginCtrl', function ($scope, $rootScope, $location, UserService, toaster) {
    var self = this;
    self.username = '';
    self.password = '';

    self.login = function () {
        UserService.authenticate(self.username, self.password).then(
            function (data) {
                var authToken = data.token;
                $rootScope.authToken = authToken;
                window.sessionStorage.removeItem('authToken');
                window.localStorage.removeItem('authToken');
                if ($rootScope.rememberMe) {
                    window.localStorage.setItem('authToken', authToken);
                } else {
                    window.sessionStorage.setItem('authToken', authToken);
                }
                self.getUser();
            },
            function (err) {
                toaster.pop('error', 'Error :', 'The password you’ve entered is incorrect.');
            }
        );
    };

    self.logout = function () {
        delete $rootScope.user;
        delete $rootScope.authToken;
        window.sessionStorage.removeItem('authToken');
        window.localStorage.removeItem('authToken');
        $location.path("/login");
    };

    self.getUser = function () {
        UserService.getUser().then(
            function (data) {
                $rootScope.user = data;
                var path = $rootScope.originalPath;
                if ($rootScope.originalPath === "/login") {
                    path = "/";
                }
                $location.path(path);
            }
        );
    };
});