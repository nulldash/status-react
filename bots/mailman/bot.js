function locationsSuggestions (params) {
    var result = {title: "Send location"};
    var seqArg = params.seqArg ? params.seqArg : "";

    if (seqArg == "Dropped pin")
    {
        result.showBack = true;
        result.height = "max";
        result.markup = ["view", {},
                            ['dropped-pin']];
    }
    else if (seqArg != "")
    {
        result.showBack = true;
        result.markup = ['scroll-view', {keyboardShouldPersistTaps: "always"},
                            ['view', {},
                                ['places-search']]];
    }
    else
    {
        result.markup = ['scroll-view', {keyboardShouldPersistTaps: "always"},
                            ['view', {},
                                ['current-location-map'],
                                ['current-location'],
                                ['separator'],
                                ['places-nearby']]];
    }

    return result;
}

status.command({
    name: "location",
    title: I18n.t('location_title'),
    description: I18n.t('location_description'),
    sequentialParams: true,
    hideSendButton: true,
    params: [{
        name: "address",
        type: status.types.TEXT,
        placeholder: I18n.t('location_address'),
        suggestions: locationsSuggestions
    }],
    preview: function (params) {
        var text = status.components.text(
            {
                style: {
                    marginTop: 0,
                    marginHorizontal: 0,
                    fontSize: 15,
                    lineHeight: 23,
                    fontFamily: "font",
                    color: "black"
                }
            }, params.address);
        var uri = "https://maps.googleapis.com/maps/api/staticmap?center="
            + params.address
            + "&size=100x100&maptype=roadmap&key=AIzaSyBNsj1qoQEYPb3IllmWMAscuXW0eeuYqAA&language=en"
            + params.address;

        var image = status.components.image(
            {
                source: {uri: uri},
                style: {
                    borderRadius: 5
                    marginTop: 12
                    height:    58
                }
            }
        );

        return {markup: ['view', {},
                            text,
                            ['view', {},
                                image,
                                ['view', {style: {position: "absolute",
                                                  top: 0,
                                                  right: 0,
                                                  bottom: 0,
                                                  left: 0,
                                                  justifyContent: "center",
                                                  alignItems: "center"}},
                                    ['view', {style: {borderColor:  "#628fe3",
                                                      backgroundColor: "#FFFFFF",
                                                      borderWidth:  4,
                                                      borderRadius: 8,
                                                      height:       15,
                                                      width:        15}}]]]]};
    },
    shortPreview: function (params) {
        return {
            markup: status.components.text(
                {},
                I18n.t('location_title') + ": " + params.address
            )
        };
    }
});