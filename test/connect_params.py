import yaml

with open('test/config.yaml', 'r') as yaml_file:
    config = yaml.load(yaml_file)

connect_params = []
param = {
    "name": "data-enrichment-latin1-sup",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=supplement,chars=¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-A",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=extendedA,chars=ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-B",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=extendedB,chars=ƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏ"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-spaceModifier",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=spaceModifier,chars=ʰʱʲʳʴʵʶʷʸʹʺʻʼʽʾʿˀˁ˂˃˄˅ˆˇˈˉˊˋˌˍˎˏːˑ˒˓˔˕˖˗˘˙˚˛˜˝˞˟ˠˡˢˣˤ˥˦˧˨˩˪˫ˬ˭ˮ˯˰˱˲˳˴˵˶˷˸˹˺˻˼˽˾˿"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-IPA",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=IPAextensions,chars=ɐɑɒɓɔɕɖɗɘəɚɛɜɝɞɟɠɡɢɣɤɥɦɧɨɩɪɫɬɭɮɯɰɱɲɳɴɵɶɷɸɹɺɻɼɽɾɿʀʁʂʃʄʅʆʇʈʉʊʋʌʍʎʏʐʑʒʓʔʕʖʗʘʙʚʛʜʝʞʟʠʡʢʣʤʥʦʧʨʩʪʫʬʭʮʯ"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-greek",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=greek,chars=ͰͱͲͳʹ͵Ͷͷ͸͹ͺͻͼͽ;Ϳ΀΁΂΃΄΅Ά·ΈΉΊ΋Ό΍ΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡ΢ΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϏϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ"
    }
}
connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-diacriticalMarks",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=diacriticalMarks,chars=	́	̂	̃	̄	̅	̆	̇	̈	̉	̊	̋	̌	̍	̎	̏	̐	̑	̒	̓	̔	̕	̖	̗	̘	̙	̚	̛	̜	̝	̞	̟	̠	̡	̢	̣	̤	̥	̦	̧	̨	̩	̪	̫	̬	̭	̮	̯	̰	̱	̲	̳	̴	̵	̶	̷	̸	̹	̺	̻	̼	̽	̾	̿	̀	́	͂	̓	̈́	ͅ	͆	͇	͈	͉	͊	͋	͌	͍	͎	͏	͐	͑	͒	͓	͔	͕	͖	͗	͘	͙	͚	͛	͜	͝	͞	͟	͠	͡	͢	ͣ	ͤ	ͥ	ͦ	ͧ	ͨ	ͩ	ͪ	ͫ	ͬ	ͭ	ͮ	ͯ"
    }
}
connect_params.append(param)
