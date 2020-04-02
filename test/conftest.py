"""
Copyright 2018-2019 Splunk, Inc..

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import pytest
import sys
import os
import json
import logging
import requests
import yaml
import time
from kafka import KafkaProducer

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(formatter)
logger.addHandler(handler)

with open('test/config.yaml', 'r') as yaml_file:
        config = yaml.load(yaml_file)

@pytest.fixture()
def setup(request):
    return config

def pytest_configure():   
    # Generate data
    producer = KafkaProducer(bootstrap_servers='localhost:9092', value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    msg = {'foo': 'bar'} 
    producer.send(config["kafka_topic"], msg)

    # Launch all connectors for tests
    connect_params = []
    param = {
        "name": "data-enrichment-latin1-sup",
        "config": {
            "connector.class": "com.splunk.kafka.connect.SplunkSinkConnector",
            "tasks.max": "1",
            "topics": config["kafka_topic"],
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
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
            "splunk.indexes": "main",
            "splunk.hec.uri": config["splunk_url"],
            "splunk.hec.token": config["splunk_token"],
            "splunk.hec.raw": "false",
            "splunk.hec.ack.enabled": "false",
            "splunk.hec.ssl.validate.certs": "false",
            "splunk.hec.json.event.enrichment" : "test=diacriticalMarks,chars=	́	̂	̃	̄	̅	̆	̇	̈	̉	̊	̋	̌	̍	̎	̏	̐	̑	̒	̓	̔	̕	̖	̗	̘	̙	̚	̛	̜	̝	̞	̟	̠	̡	̢	̣	̤	̥	̦	̧	̨	̩	̪	̫	̬	̭	̮	̯	̰	̱	̲	̳	̴	̵	̶	̷	̸	̹	̺	̻	̼	̽	̾	̿	̀	́	͂	̓	̈́	ͅ	͆	͇	͈	͉	͊	͋	͌	͍	͎	͏	͐	͑	͒	͓	͔	͕	͖	͗	͘	͙	͚	͛	͜	͝	͞	͟	͠	͡	͢	ͣ	ͤ	ͥ	ͦ	ͧ	ͨ	ͩ	ͪ	ͫ	ͬ	ͭ	ͮ	ͯ"
        }
    }
    connect_params.append(param)
    
    for param in connect_params:
        response = requests.post(url=config["kafka_connect_url"]+"/connectors", data=json.dumps(param),
                        headers={'Accept': 'application/json', 'Content-Type': 'application/json'})

        if response.status_code == 201:
            logger.info("Created connector successfully - " + json.dumps(params))   
        else:
            logger.error("failed to create connector", param)
            print(response)
    # wait for data to be ingested to Splunk
    time.sleep(30)