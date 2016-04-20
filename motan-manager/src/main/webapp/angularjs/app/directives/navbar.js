//Sidebar Collapse
angular.module('app')
    .directive('sidebarCollapse', function () {
        return {
            restrict: 'AC',
            template: '<i class="collapse-icon fa fa-bars"></i>',
            link: function (scope, el, attr) {
                el.on('click', function () {
                    if (!$('#sidebar').is(':visible'))
                        $("#sidebar").toggleClass("hide");
                    $("#sidebar").toggleClass("menu-compact");
                    $(".sidebar-collapse").toggleClass("active");
                    var isCompact = $("#sidebar").hasClass("menu-compact");

                    if ($(".sidebar-menu").closest("div").hasClass("slimScrollDiv")) {
                        $(".sidebar-menu").slimScroll({ destroy: true });
                        $(".sidebar-menu").attr('style', '');
                    }
                    if (isCompact) {
                        $(".open > .submenu")
                            .removeClass("open");
                    } else {
                        if ($('.page-sidebar').hasClass('sidebar-fixed')) {
                            var position = (readCookie("rtl-support") || location.pathname == "/index-rtl-fa.html" || location.pathname == "/index-rtl-ar.html") ? 'right' : 'left';
                            $('.sidebar-menu').slimscroll({
                                height: $(window).height() - 90,
                                position: position,
                                size: '3px',
                                color: themeprimary
                            });
                        }
                    }
                    //Slim Scroll Handle
                });
            }
        };
    });

//Setting
angular.module('app')
    .directive('setting', function () {
        return {
            restrict: 'AC',
            template: '<a id="btn-setting" title="Setting" href="#"><i class="icon glyphicon glyphicon-cog"></i></a>',
            link: function (scope, el, attr) {
                el.on('click', function () {
                    $('.navbar-account').toggleClass('setting-open');
                });
            }
        };
    });
