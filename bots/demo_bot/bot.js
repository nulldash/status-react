function round(n) {
    return Math.round(n * 100) / 100;
}

function doubledValueLabel(params) {
    var value = round(params.value);
    return "sliderValue = " + value +
        "; (2 * sliderValue) = " + (2 * value);
}

status.defineSubscription(
    // the name of subscription and the name of the value in bot-db
    // associated with this subscription
    "doubledValue",
    // the map of values on which subscription depends: keys are arbitrary names
    // and values are db paths to another value
    {value: ["sliderValue"]},
    // the function which will be called as reaction on changes of values above,
    // should be pure. Returned result will be associated with subscription in bot-db
    doubledValueLabel
);

status.defineSubscription(
    "roundedValue",
    {value: ["sliderValue"]},
    function (params) {
        return round(params.value);
    }
);

function superSuggestion(params, context) {
    var balance = parseFloat(web3.fromWei(web3.eth.getBalance(context.from), "ether"));
    var defaultSliderValue = balance / 2;

    var view = ["view", {},
        ["text", {}, "Balance " + balance + " ETH"],
        ["text", {}, ["subscribe", ["doubledValue"]]],
        ["slider", {
            maximumValue: ["subscribe", ["balance"]],
            value: defaultSliderValue,
            minimumValue: 0,
            onSlidingComplete: ["dispatch", ["set", "sliderValue"]],
            step: 0.05
        }],
        ['touchable',
            {onPress: ['dispatch', ["set-value-from-db", "roundedValue"]]},
            ["view", {}, ["text", {}, "Set value"]]
        ],
        ["text", {style: {color: "red"}}, ["subscribe", ["validationText"]]]
    ];

    status.setDefaultDb({
        sliderValue: defaultSliderValue,
        doubledValue: doubledValueLabel({value: defaultSliderValue})
    });

    var validationText = "";

    if (isNaN(params.message)) {
        validationText = "That's not a float number!";
    } else if (parseFloat(params.message) > balance) {
        validationText =
            "Input value is too big!" +
            " You have only " + balance + " ETH on your balance!";
    }

    status.updateDb({
        balance: balance,
        validationText: validationText
    });

    return {markup: view};
};

status.addListener("on-message-input-change", superSuggestion);
status.addListener("on-message-send", function (params, context) {
    if (isNaN(params.message)) {
        return {"text-message": "Seems that you don't want to send money :("};
    }

    var balance = web3.eth.getBalance(context.from);
    var value = parseFloat(params.message);
    var weiValue = web3.toWei(value, "ether");
    if (bn(weiValue).greaterThan(bn(balance))) {
        return {"text-message": "No way man, you don't have enough money! :)"};
    }
    try {
        web3.eth.sendTransaction({
            from: context.from,
            to: context.from,
            value: weiValue
        });
        return {"text-message": "You are the hero, you sent " + value + " ETH to yourself!"};
    } catch (err) {
        return {"text-message": "Something went wrong :("};
    }
});
