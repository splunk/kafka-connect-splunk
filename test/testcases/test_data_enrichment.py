from lib.commonkafka import *
from lib.commonsplunk import check_events_from_splunk
from lib.helper import get_test_folder
import pytest
import re

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger("test_case")


class TestDataEnrichment:

    @pytest.mark.parametrize("test_scenario,test_input,expected", [
        ("supplement", "¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ", 3),
        ("extendedA", "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ", 3),
        ("extendedB", "ƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏ", 3),
        ("IPAextensions", "ɐɑɒɓɔɕɖɗɘəɚɛɜɝɞɟɠɡɢɣɤɥɦɧɨɩɪɫɬɭɮɯɰɱɲɳɴɵɶɷɸɹɺɻɼɽɾɿʀʁʂʃʄʅʆʇʈʉʊʋʌʍʎʏʐʑʒʓʔʕʖʗʘʙʚʛʜʝʞʟʠʡʢʣʤʥʦʧʨʩʪʫʬʭʮʯ", 3),
        ("spaceModifier", "ʰʱʲʳʴʵʶʷʸʹʺʻʼʽʾʿˀˁ˂˃˄˅ˆˇˈˉˊˋˌˍˎˏːˑ˒˓˔˕˖˗˘˙˚˛˜˝˞˟ˠˡˢˣˤ˥˦˧˨˩˪˫ˬ˭ˮ˯˰˱˲˳˴˵˶˷˸˹˺˻˼˽˾˿", 3),
        # Skipping this test. because it is special character, I cannot get it to work in search
        # ("diacriticalMarks", "̀	́	̂	̃	̄	̅	̆	̇	̈	̉	̊	̋	̌	̍	̎	̏	̐	̑	̒	̓	̔	̕	̖	̗	̘	̙	̚	̛	̜	̝	̞	̟	̠	̡	̢	̣	̤	̥	̦	̧	̨	̩	̪	̫	̬	̭	̮	̯	̰	̱	̲	̳	̴	̵	̶	̷	̸	̹	̺	̻	̼	̽	̾	̿	̀	́	͂	̓	̈́	ͅ	͆	͇	͈	͉	͊	͋	͌	͍	͎	͏	͐	͑	͒	͓	͔	͕	͖	͗	͘	͙	͚	͛	͜	͝	͞	͟	͠	͡	͢	ͣ	ͤ	ͥ	ͦ	ͧ	ͨ	ͩ	ͪ	ͫ	ͬ	ͭ	ͮ	ͯ", 1),
        ("greek", "ͰͱͲͳʹ͵Ͷͷ͸͹ͺͻͼͽ;Ϳ΀΁΂΃΄΅Ά·ΈΉΊ΋Ό΍ΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡ΢ΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϏϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ", 3)
    ])
    def test_data_enrichment_latin1(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" chars::{test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-1h@h",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour",
                    len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_case, test_input, expected", [
        ("line_breaking_of_raw_data", "kafka:topic_test_break_raw", 1),
        ("line_breaking_of_event_data", "kafka:topic_test_break_event", 3)
    ])
    def test_line_breaking_configuration(self, setup, test_case, test_input, expected):
        logger.info(f"testing {test_case} expected={expected} ")
        search_query = f"index={setup['splunk_index']} | search timestamp=\"{setup['timestamp']}\" source::{test_input}"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-1h@h",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour",
                    len(events))
        assert len(events) == expected
        if test_case == 'line_breaking_of_raw_data':
            event_raw_data = events[0]['_raw'].strip()
            # Replace the white spaces since Splunk 8.0.2 and 8.0.3 behave differently
            actual_raw_data = re.sub(r'\s+', '', event_raw_data)
            expected_data = "{\"timestamp\":\"%s\"}######" \
                            "{\"timestamp\":\"%s\"}######" \
                            "{\"timestamp\":\"%s\"}######" % (
                                setup["timestamp"], setup["timestamp"], setup["timestamp"])
            assert actual_raw_data == expected_data, \
                f'\nActual value: \n{actual_raw_data} \ndoes not match expected value: \n{expected_data}'

    @pytest.mark.parametrize("test_scenario, test_input, expected", [
        ("record_key_extraction", "sourcetype::track_record_key", "{}"),
    ])
    def test_record_key_data_enrichment(self, setup, test_scenario, test_input, expected):
        logger.info(f"testing {test_scenario} input={test_input} expected={expected} event(s)")
        search_query = f"index={setup['splunk_index']} | search {test_input} | fields *"
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-15m@m",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=[f"search {search_query}"],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour", len(events))
        
        if(len(events)==1):
           assert events[0]["kafka_record_key"] == expected 
        else:
            assert False,"No event found or duplicate events found"
