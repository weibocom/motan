angular.module('app')
  .directive('loadingContainer', function () {
      return {
          restrict: 'AC',
          link: function (scope, el, attrs) {
              el.removeClass('app-loading');
              scope.$on('$stateChangeStart', function (event) {
                  el.addClass('app-loading');
              });
              scope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState) {
                  event.targetScope.$watch('$viewContentLoaded', function() {
                      el.removeClass('app-loading ');
                  });
              });
          }
      };
  });