angular.module('app').directive("getSinaUserlongQPS", [
    function () {
        return {
            restrict: "AE",
            link: function (scope, ele) {
                var target = "alias(stats.openapi.profile.userrpc_core.yf.SERVICE.cn_sina_api_data_service_SinaUserService_getSinaUserlong.total_count,'yf_qps')",
                    target2 = "alias(stats.openapi.profile.userrpc_core.tc.SERVICE.cn_sina_api_data_service_SinaUserService_getSinaUserlong.total_count,'tc_qps')",
                    updateInterval = 5000,
                    plot = null,
                    series = [{
                        data: [],
                        label: "yf_qps"
                        //lines: {fill: true}
                    },{
                        data: [],
                        label: "tc_qps"
                        //lines: {fill: true}
                    }];

                function createPlot() {
                    return $.plot(ele[0], series, {
                        yaxis: {
                            color: '#f3f3f3'
                        },
                        xaxis: {
                            color: '#f3f3f3',
                            mode: "time",
                            timeformat: "%H:%M",
                            timezone: "browser"
                        },
                        grid: {
                            hoverable: true,
                            clickable: false,
                            borderWidth: 0,
                            aboveData: false
                        },
                        colors: [scope.settings.color.themeprimary],
                        series: {
                            lines: {
                                fill: true,
                                fillColor: {
                                    colors: [{
                                        opacity: 0.4
                                    }, {
                                        opacity: 0
                                    }]
                                },
                                steps: false
                            },
                            shadowSize: 0
                        },
                        legend: {
                            position: "nw"
                        },
                        tooltip: true, //boolean
                        tooltipOpts: {
                            content: "<b>Time: %x</b>, <b>QPS: </b><span>%y.0</span>", //%s -> series label, %x -> X value, %y -> Y value, %x.2 -> precision of X value, %p -> percent
                            dateFormat: "%m/%d %H:%M",
                            defaultTheme: true
                        }
                    });
                }

                function updatePlot(target) {
                    var query_url = "http://10.13.81.28/render?format=json&from=-2hours&until=now&target=" + target;
                    $.getJSON(query_url, function (targets) {
                        if (targets.length <= 0) return;
                        var datapoints = targets[0].datapoints;
                        var xzero = datapoints[0][1];

                        var data = $.map(datapoints, function (value) {
                            if (value[0] === null) return null;
                            // hack of $.map will flat array object
                            //return [[ value[1]-xzero, value[0] ]];
                            return [[value[1] + "000", value[0]]];
                        });
                        // replace null value with previous item value
                        for (var i = 0; i < data.length; i++) {
                            if (i > 0 && data[i] === null) data[i] = data[-i];
                        }

                        series[0].data = data;
                    });
                    $.getJSON("http://10.13.81.28/render?format=json&from=-2hours&until=now&target=" + target2, function (targets) {
                        if (targets.length <= 0) return;
                        var datapoints = targets[0].datapoints;
                        var xzero = datapoints[0][1];

                        var data = $.map(datapoints, function (value) {
                            if (value[0] === null) return null;
                            // hack of $.map will flat array object
                            //return [[ value[1]-xzero, value[0] ]];
                            return [[value[1] + "000", value[0]]];
                        });
                        // replace null value with previous item value
                        for (var i = 0; i < data.length; i++) {
                            if (i > 0 && data[i] === null) data[i] = data[-i];
                        }

                        series[1].data = data;
                    });
                    if (plot === null) {
                        plot = createPlot();
                    } else {
                        plot.setData(series);
                        plot.setupGrid();
                        plot.draw();
                    }
                }

                updatePlot(target);
                setInterval(function () {
                    updatePlot(target);
                }, updateInterval);
            }
        };
    }
]);
