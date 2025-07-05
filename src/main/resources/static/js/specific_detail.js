(function ($) {

	"use strict";

// Sticky sidebar
$('#sidebar_fixed').theiaStickySidebar({
    minWidth: 991,
    updateSidebarHeight: true,
    additionalMarginTop: 25
});

// Read more 
$(".content_more").hide();
    $(".show_hide").on("click", function () {
        var txt = $(".content_more").is(':visible') ? 'Read More' : 'Read Less';
        $(this).text(txt);
        $(this).prev('.content_more').slideToggle(200);
});

// Time and people select
$('.radio_select input[type="radio"]').on("click", function () {
    var value = $("input[name='time']:checked").val();
    $('#selected_time').text(value);
});

$('.radio_select input[type="radio"]').on("click", function (){
    var value = $("input[name='people']:checked").val();
    $('#selected_people').text(value);
});

$('.radio_select input[type="radio"]').on("click", function (){
    var value = $("input[name='day']:checked").val();
    $('#selected_day').text(value);
});

// Image popups
	$('.magnific-gallery').each(function () {
		$(this).magnificPopup({
			delegate: 'a',
			type: 'image',
            preloader: true,
			gallery: {
				enabled: true
			},
			removalDelay: 500, //delay removal by X to allow out-animation
			callbacks: {
				beforeOpen: function () {
					// just a hack that adds mfp-anim class to markup 
					this.st.image.markup = this.st.image.markup.replace('mfp-figure', 'mfp-figure mfp-with-anim');
					this.st.mainClass = this.st.el.attr('data-effect');
				}
			},
			closeOnContentClick: true,
			midClick: true // allow opening popup on middle mouse click. Always set it to true if you don't provide alternative source.
		});
	});

// Image popups menu
	$('.menu-gallery').each(function () {
		$(this).magnificPopup({
			delegate: 'figure a',
			type: 'image',
            preloader: true,
			gallery: {
				enabled: true
			},
			removalDelay: 500, //delay removal by X to allow out-animation
			callbacks: {
				beforeOpen: function () {
					// just a hack that adds mfp-anim class to markup 
					this.st.image.markup = this.st.image.markup.replace('mfp-figure', 'mfp-figure mfp-with-anim');
					this.st.mainClass = this.st.el.attr('data-effect');
				}
			},
			closeOnContentClick: true,
			midClick: true // allow opening popup on middle mouse click. Always set it to true if you don't provide alternative source.
		});
	});

// Reserve Fixed on mobile
$('.btn_reserve_fixed a').on('click', function() {
     $(".box_booking").show();
});
$(".close_panel_mobile").on('click', function (event){
    event.stopPropagation();
    $(".box_booking").hide();
  });

})(window.jQuery); 