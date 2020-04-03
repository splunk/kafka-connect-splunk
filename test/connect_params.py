import yaml

with open('test/config.yaml', 'r') as yaml_file:
    config = yaml.load(yaml_file)

connect_params = []
data_enrichment_connect_params=[]
data_onboarding_connect_params=[]

param = {
    "name": "data-enrichment-latin1-sup",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=supplement,chars=¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-A",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=extendedA,chars=ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-B",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=extendedB,chars=ƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏ"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-spaceModifier",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=spaceModifier,chars=ʰʱʲʳʴʵʶʷʸʹʺʻʼʽʾʿˀˁ˂˃˄˅ˆˇˈˉˊˋˌˍˎˏːˑ˒˓˔˕˖˗˘˙˚˛˜˝˞˟ˠˡˢˣˤ˥˦˧˨˩˪˫ˬ˭ˮ˯˰˱˲˳˴˵˶˷˸˹˺˻˼˽˾˿"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-IPA",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=IPAextensions,chars=ɐɑɒɓɔɕɖɗɘəɚɛɜɝɞɟɠɡɢɣɤɥɦɧɨɩɪɫɬɭɮɯɰɱɲɳɴɵɶɷɸɹɺɻɼɽɾɿʀʁʂʃʄʅʆʇʈʉʊʋʌʍʎʏʐʑʒʓʔʕʖʗʘʙʚʛʜʝʞʟʠʡʢʣʤʥʦʧʨʩʪʫʬʭʮʯ"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-greek",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=greek,chars=ͰͱͲͳʹ͵Ͷͷ͸͹ͺͻͼͽ;Ϳ΀΁΂΃΄΅Ά·ΈΉΊ΋Ό΍ΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡ΢ΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϏϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ"
    }
}
data_enrichment_connect_params.append(param)
param = {
    "name": "data-enrichment-latin1-diacriticalMarks",
    "config": {
        "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
        "tasks.max": "1",
        "topics": config["kafka_topic"],
        "splunk.indexes": config["splunk_index"],
        "splunk.hec.uri": config["splunk_hec_url"],
        "splunk.hec.token": config["splunk_token"],
        "splunk.hec.raw": "false",
        "splunk.hec.ack.enabled": "false",
        "splunk.hec.ssl.validate.certs": "false",
        "splunk.hec.json.event.enrichment" : "test=diacriticalMarks,chars=	́	̂	̃	̄	̅	̆	̇	̈	̉	̊	̋	̌	̍	̎	̏	̐	̑	̒	̓	̔	̕	̖	̗	̘	̙	̚	̛	̜	̝	̞	̟	̠	̡	̢	̣	̤	̥	̦	̧	̨	̩	̪	̫	̬	̭	̮	̯	̰	̱	̲	̳	̴	̵	̶	̷	̸	̹	̺	̻	̼	̽	̾	̿	̀	́	͂	̓	̈́	ͅ	͆	͇	͈	͉	͊	͋	͌	͍	͎	͏	͐	͑	͒	͓	͔	͕	͖	͗	͘	͙	͚	͛	͜	͝	͞	͟	͠	͡	͢	ͣ	ͤ	ͥ	ͦ	ͧ	ͨ	ͩ	ͪ	ͫ	ͬ	ͭ	ͮ	ͯ"
    }
}
data_enrichment_connect_params.append(param)

connect_params.extend(data_enrichment_connect_params)
connect_params.extend(data_onboarding_connect_params)