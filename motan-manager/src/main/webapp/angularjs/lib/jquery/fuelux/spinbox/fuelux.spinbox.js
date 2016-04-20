/*
 * Fuel UX Spinbox
 * https://github.com/ExactTarget/fuelux
 *
 * Copyright (c) 2014 ExactTarget
 * Licensed under the BSD New license.
 */

// -- BEGIN UMD WRAPPER PREFACE --

// For more information on UMD visit:
// https://github.com/umdjs/umd/blob/master/jqueryPlugin.js

(function (factory) {
	if (typeof define === 'function' && define.amd) {
		// if AMD loader is available, register as an anonymous module.
		define(['jquery'], factory);
	} else {
		// OR use browser globals if AMD is not present
		factory(jQuery);
	}
}(function ($) {
	// -- END UMD WRAPPER PREFACE --

	// -- BEGIN MODULE CODE HERE --

	var old = $.fn.spinbox;

	// SPINBOX CONSTRUCTOR AND PROTOTYPE

	var Spinbox = function (element, options) {
		this.$element = $(element);
		this.$element.find('.btn').on('click', function (e) {
			//keep spinbox from submitting if they forgot to say type="button" on their spinner buttons
			e.preventDefault();
		});
		this.options = $.extend({}, $.fn.spinbox.defaults, options);
		this.$input = this.$element.find('.spinbox-input');
		this.$element.on('focusin.fu.spinbox', this.$input, $.proxy(this.changeFlag, this));
		this.$element.on('focusout.fu.spinbox', this.$input, $.proxy(this.change, this));
		this.$element.on('keydown.fu.spinbox', this.$input, $.proxy(this.keydown, this));
		this.$element.on('keyup.fu.spinbox', this.$input, $.proxy(this.keyup, this));

		this.bindMousewheelListeners();
		this.mousewheelTimeout = {};

		if (this.options.hold) {
			this.$element.on('mousedown.fu.spinbox', '.spinbox-up', $.proxy(function () {
				this.startSpin(true);
			}, this));
			this.$element.on('mouseup.fu.spinbox', '.spinbox-up, .spinbox-down', $.proxy(this.stopSpin, this));
			this.$element.on('mouseout.fu.spinbox', '.spinbox-up, .spinbox-down', $.proxy(this.stopSpin, this));
			this.$element.on('mousedown.fu.spinbox', '.spinbox-down', $.proxy(function () {
				this.startSpin(false);
			}, this));
		} else {
			this.$element.on('click.fu.spinbox', '.spinbox-up', $.proxy(function () {
				this.step(true);
			}, this));
			this.$element.on('click.fu.spinbox', '.spinbox-down', $.proxy(function () {
				this.step(false);
			}, this));
		}

		this.switches = {
			count: 1,
			enabled: true
		};

		if (this.options.speed === 'medium') {
			this.switches.speed = 300;
		} else if (this.options.speed === 'fast') {
			this.switches.speed = 100;
		} else {
			this.switches.speed = 500;
		}

		this.lastValue = this.options.value;

		this.render();

		if (this.options.disabled) {
			this.disable();
		}
	};

	Spinbox.prototype = {
		constructor: Spinbox,

		destroy: function () {
			this.$element.remove();
			// any external bindings
			// [none]
			// set input value attrbute
			this.$element.find('input').each(function () {
				$(this).attr('value', $(this).val());
			});
			// empty elements to return to original markup
			// [none]
			// returns string of markup
			return this.$element[0].outerHTML;
		},

		render: function () {
			var inputValue = this.parseInput(this.$input.val());
			var maxUnitLength = '';

			// if input is empty and option value is default, 0
			if (inputValue !== '' && this.options.value === 0) {
				this.value(inputValue);
			} else {
				this.output(this.options.value);
			}

			if (this.options.units.length) {
				$.each(this.options.units, function (index, value) {
					if (value.length > maxUnitLength.length) {
						maxUnitLength = value;
					}
				});
			}

		},

		output: function (value, updateField) {
			value = (value + '').split('.').join(this.options.decimalMark);
			updateField = (updateField || true);
			if (updateField) {
				this.$input.val(value);
			}

			return value;
		},

		parseInput: function (value) {
			value = (value + '').split(this.options.decimalMark).join('.');

			return value;
		},

		change: function () {
			var newVal = this.parseInput(this.$input.val()) || '';

			if (this.options.units.length || this.options.decimalMark !== '.') {
				newVal = this.parseValueWithUnit(newVal);
			} else if (newVal / 1) {
				newVal = this.options.value = this.checkMaxMin(newVal / 1);
			} else {
				newVal = this.checkMaxMin(newVal.replace(/[^0-9.-]/g, '') || '');
				this.options.value = newVal / 1;
			}
			this.output(newVal);

			this.changeFlag = false;
			this.triggerChangedEvent();
		},

		changeFlag: function () {
			this.changeFlag = true;
		},

		stopSpin: function () {
			if (this.switches.timeout !== undefined) {
				clearTimeout(this.switches.timeout);
				this.switches.count = 1;
				this.triggerChangedEvent();
			}
		},

		triggerChangedEvent: function () {
			var currentValue = this.value();
			if (currentValue === this.lastValue) return;

			this.lastValue = currentValue;

			// Primary changed event
			this.$element.trigger('changed.fu.spinbox', this.output(currentValue, false)); // no DOM update
		},

		startSpin: function (type) {

			if (!this.options.disabled) {
				var divisor = this.switches.count;

				if (divisor === 1) {
					this.step(type);
					divisor = 1;
				} else if (divisor < 3) {
					divisor = 1.5;
				} else if (divisor < 8) {
					divisor = 2.5;
				} else {
					divisor = 4;
				}

				this.switches.timeout = setTimeout($.proxy(function () {
					this.iterate(type);
				}, this), this.switches.speed / divisor);
				this.switches.count++;
			}
		},

		iterate: function (type) {
			this.step(type);
			this.startSpin(type);
		},

		step: function (isIncrease) {
			// isIncrease: true is up, false is down

			var digits, multiple, currentValue, limitValue;

			// trigger change event
			if (this.changeFlag) {
				this.change();
			}

			// get current value and min/max options
			currentValue = this.options.value;
			limitValue = isIncrease ? this.options.max : this.options.min;

			if ((isIncrease ? currentValue < limitValue : currentValue > limitValue)) {
				var newVal = currentValue + (isIncrease ? 1 : -1) * this.options.step;

				// raise to power of 10 x number of decimal places, then round
				if (this.options.step % 1 !== 0) {
					digits = (this.options.step + '').split('.')[1].length;
					multiple = Math.pow(10, digits);
					newVal = Math.round(newVal * multiple) / multiple;
				}

				// if outside limits, set to limit value
				if (isIncrease ? newVal > limitValue : newVal < limitValue) {
					this.value(limitValue);
				} else {
					this.value(newVal);
				}

			} else if (this.options.cycle) {
				var cycleVal = isIncrease ? this.options.min : this.options.max;
				this.value(cycleVal);
			}
		},

		value: function (value) {

			if (value || value === 0) {
				if (this.options.units.length || this.options.decimalMark !== '.') {
					this.output(this.parseValueWithUnit(value + (this.unit || '')));
					return this;

				} else if (!isNaN(parseFloat(value)) && isFinite(value)) {
					this.options.value = value / 1;
					this.output(value + (this.unit ? this.unit : ''));
					return this;

				}
			} else {
				if (this.changeFlag) {
					this.change();
				}

				if (this.unit) {
					return this.options.value + this.unit;
				} else {
					return this.output(this.options.value, false); // no DOM update
				}
			}
		},

		isUnitLegal: function (unit) {
			var legalUnit;

			$.each(this.options.units, function (index, value) {
				if (value.toLowerCase() === unit.toLowerCase()) {
					legalUnit = unit.toLowerCase();
					return false;
				}
			});

			return legalUnit;
		},

		// strips units and add them back
		parseValueWithUnit: function (value) {
			var unit = value.replace(/[^a-zA-Z]/g, '');
			var number = value.replace(/[^0-9.-]/g, '');

			if (unit) {
				unit = this.isUnitLegal(unit);
			}

			this.options.value = this.checkMaxMin(number / 1);
			this.unit = unit || undefined;
			return this.options.value + (unit || '');
		},

		checkMaxMin: function (value) {
			// if unreadable
			if (isNaN(parseFloat(value))) {
				return value;
			}
			// if not within range return the limit
			if (!(value <= this.options.max && value >= this.options.min)) {
				value = value >= this.options.max ? this.options.max : this.options.min;
			}
			return value;
		},

		disable: function () {
			this.options.disabled = true;
			this.$element.addClass('disabled');
			this.$input.attr('disabled', '');
			this.$element.find('button').addClass('disabled');
		},

		enable: function () {
			this.options.disabled = false;
			this.$element.removeClass('disabled');
			this.$input.removeAttr('disabled');
			this.$element.find('button').removeClass('disabled');
		},

		keydown: function (event) {
			var keyCode = event.keyCode;
			if (keyCode === 38) {
				this.step(true);
			} else if (keyCode === 40) {
				this.step(false);
			}
		},

		keyup: function (event) {
			var keyCode = event.keyCode;

			if (keyCode === 38 || keyCode === 40) {
				this.triggerChangedEvent();
			}
		},

		bindMousewheelListeners: function () {
			var inputEl = this.$input.get(0);
			if (inputEl.addEventListener) {
				//IE 9, Chrome, Safari, Opera
				inputEl.addEventListener('mousewheel', $.proxy(this.mousewheelHandler, this), false);
				// Firefox
				inputEl.addEventListener('DOMMouseScroll', $.proxy(this.mousewheelHandler, this), false);
			} else {
				// IE <9
				inputEl.attachEvent('onmousewheel', $.proxy(this.mousewheelHandler, this));
			}
		},

		mousewheelHandler: function (event) {
			if (!this.options.disabled) {
				var e = window.event || event; // old IE support
				var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail)));
				var self = this;

				clearTimeout(this.mousewheelTimeout);
				this.mousewheelTimeout = setTimeout(function () {
					self.triggerChangedEvent();
				}, 300);

				if (delta < 0) {
					this.step(true);
				} else {
					this.step(false);
				}

				if (e.preventDefault) {
					e.preventDefault();
				} else {
					e.returnValue = false;
				}
				return false;
			}
		}
	};


	// SPINBOX PLUGIN DEFINITION

	$.fn.spinbox = function (option) {
		var args = Array.prototype.slice.call(arguments, 1);
		var methodReturn;

		var $set = this.each(function () {
			var $this = $(this);
			var data = $this.data('fu.spinbox');
			var options = typeof option === 'object' && option;

			if (!data) {
				$this.data('fu.spinbox', (data = new Spinbox(this, options)));
			}
			if (typeof option === 'string') {
				methodReturn = data[option].apply(data, args);
			}
		});

		return (methodReturn === undefined) ? $set : methodReturn;
	};

	// value needs to be 0 for this.render();
	$.fn.spinbox.defaults = {
		value: 1,
		min: 0,
		max: 10,
		step: 1,
		hold: true,
		speed: 'medium',
		disabled: false,
		cycle: false,
		units: [],
		decimalMark: '.'
	};

	$.fn.spinbox.Constructor = Spinbox;

	$.fn.spinbox.noConflict = function () {
		$.fn.spinbox = old;
		return this;
	};


	// DATA-API

	$(document).on('mousedown.fu.spinbox.data-api', '[data-initialize=spinbox]', function (e) {
		var $control = $(e.target).closest('.spinbox');
		if (!$control.data('fu.spinbox')) {
			$control.spinbox($control.data());
		}
	});

	// Must be domReady for AMD compatibility
	$(function () {
		$('[data-initialize=spinbox]').each(function () {
			var $this = $(this);
			if (!$this.data('fu.spinbox')) {
				$this.spinbox($this.data());
			}
		});
	});

	// -- BEGIN UMD WRAPPER AFTERWORD --
}));
// -- END UMD WRAPPER AFTERWORD --
