(function($){
    $(function(){
        $('.button-collapse').sideNav();
        var lineDrawing = anime({
            targets: '#lineDrawing .lines path',
            strokeDashoffset: [anime.setDashoffset, 0],
            easing: 'easeInOutSine',
            duration: 1500,
            delay: function(el, i) { return i * 25 },
            direction: 'alternate',
            loop: false
        });
    }); // end of document ready
})(jQuery); // end of jQuery name space