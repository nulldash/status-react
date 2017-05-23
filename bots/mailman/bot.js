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
                    marginTop: 5,
                    marginHorizontal: 0,
                    fontSize: 14,
                    fontFamily: "font",
                    color: "black"
                }
            }, params.address);
        var uri = "https://maps.googleapis.com/maps/api/staticmap?center="
            + params.address
            + "&size=100x100&maptype=roadmap&key=AIzaSyBNsj1qoQEYPb3IllmWMAscuXW0eeuYqAA&language=en"
            + "&markers=size:mid%7Ccolor:0xff0000%7Clabel:%7C"
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

        return {markup: status.components.view({}, [text, image])};
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