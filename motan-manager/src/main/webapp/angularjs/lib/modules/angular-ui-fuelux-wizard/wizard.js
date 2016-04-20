angular.module("fuelux.wizard", [])
    .directive('wizard', ['$compile', '$parse', function ($compile, $parse) {
        return {
            terminal: true, priority: 1000,
            restrict: 'A',
            scope: true,//{change: '&',    changed: '&', wizard:'='},
            link: function (scope, ele, attrs) {
                ele.removeAttr("wizard");
                var steps = ele.find(".wizard > .steps > li"), stepContents = ele.find(".step-content > .step-pane"), stepEle;

                scope.attrWiz = attrs.wizard;
                scope.currentStepIndex = (+scope.$eval(scope.attrWiz));
                scope.currentStepIndex = scope.currentStepIndex > 0 ? scope.currentStepIndex : 0;
                for (var step = 0; step < steps.length; step++) {//register steps
                    scope.steps.push({currentStep: false});
                    stepEle = angular.element(steps[step]).attr('ng-class', '{active:steps[' + step + '].currentStep, complete:' + step + '<currentStepIndex}');
                    stepEle.attr('ng-click', 'stepClicked(' + step + ')');
                    stepEle.find('span.step').attr('ng-class', '{\'step-info\':steps[' + step + '].currentStep, \'step-success\':' + step + '<currentStepIndex}');

                    angular.element(stepContents[step]).attr('ng-class', '{active:steps[' + step + '].currentStep}');
                }
                ele.find('button.btn-prev').attr('ng-click', 'showPreviousStep()').attr('ng-disabled', '!hasPrevious()');
                ele.find('button.btn-next').attr('ng-click', 'showNextStep()').attr('ng-disabled', '!hasNext() && !isFinish()');

                scope.steps[scope.currentStepIndex].currentStep = true;
                $compile(ele)(scope);

                scope.change = $parse(attrs.change);
                scope.changed = $parse(attrs.changed);
                scope.finish = $parse(attrs.finish);
                scope.$watch(attrs.wizard, function (stepValue) {
                    if (stepValue != scope.currentStepIndex) {
                        scope.toggleSteps(stepValue);
                    }
                });
            },

            controller: function ($scope) {
                $scope.steps = [];
                this.registerStep = function (step) {
                    $scope.steps.push(step);
                };

                $scope.toggleSteps = function (showIndex, direction) {
                    var event = {event: {fromStep: $scope.currentStepIndex, toStep: showIndex}};
                    if ($scope.isFinish() && direction === true) {
                        event.event.toStep = 'End';
                        if ($scope.finish && $scope.finish($scope, event)) return;
                        return;
                    }
                    if (direction === true || direction === false) {
                        event.event.direction = (direction === true ? 'next' : 'prev');
                        if ($scope.change && $scope.change($scope, event)) return;
                    }

                    $scope.steps[$scope.currentStepIndex].currentStep = false;
                    $scope.currentStepIndex = showIndex;
                    $scope.$parent.$eval($scope.attrWiz + ' = ' + showIndex);

                    $scope.steps[$scope.currentStepIndex].currentStep = true;
                    if ($scope.changed) {
                        $scope.changed($scope, event);
                    }
                };
                $scope.stepClicked = function ($index) {
                    if ($index < $scope.currentStepIndex) $scope.toggleSteps($index);
                };
                $scope.showNextStep = function () {
                    $scope.toggleSteps($scope.currentStepIndex + 1, true);
                };
                $scope.showPreviousStep = function () {
                    $scope.toggleSteps($scope.currentStepIndex - 1, false);
                };
                $scope.hasNext = function () {
                    return $scope.currentStepIndex < ($scope.steps.length - 1);
                };
                $scope.hasPrevious = function () {
                    return $scope.currentStepIndex > 0;
                };
                $scope.isFinish = function () {
                    return $scope.currentStepIndex == ($scope.steps.length - 1);
                };
            }
        };
    }]);
