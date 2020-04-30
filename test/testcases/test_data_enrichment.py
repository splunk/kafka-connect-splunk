from lib.commonkafka import *
from lib.commonsplunk import check_events_from_splunk
from lib.helper import get_test_folder
import pytest
import re

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger(__name__)


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
        logger.info("testing {0} input={1} expected={2} event(s)".format(
            test_scenario, test_input, expected))
        search_query = "index={0} | search timestamp=\"{1}\" chars::{2}".format(setup['splunk_index'],
                                                                                setup["timestamp"],
                                                                                test_input)
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-1h@h",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=["search {0}".format(
                                              search_query)],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour",
                    len(events))
        assert len(events) == expected

    @pytest.mark.parametrize("test_case, expected", [
        ("line_breaking_of_raw_events", 1)
    ])
    def test_line_breaking_of_raw_events(self, setup, test_case, expected):
        logger.info("testing {0} expected={1} ".format(test_case, expected))
        search_query = "index={0} | search timestamp=\"{1}\" source::{2}".format(setup['splunk_index'],
                                                                                         setup["timestamp"],
                                                                                         "kafka:topictestbreak")
        logger.info(search_query)
        events = check_events_from_splunk(start_time="-1h@h",
                                          url=setup["splunkd_url"],
                                          user=setup["splunk_user"],
                                          query=["search {0}".format(
                                              search_query)],
                                          password=setup["splunk_password"])
        logger.info("Splunk received %s events in the last hour",
                    len(events))
        assert len(events) == expected
        event_raw_data = events[0]['_raw'].strip()
        # Replace the white spaces since Splunk 8.0.2 and 8.0.3 behave differently
        actual_raw_data = re.sub(r'\s+', '', event_raw_data)
        expected_data = "{\"timestamp\":\"%s\"}######" \
                        "{\"timestamp\":\"%s\"}######" \
                        "{\"timestamp\":\"%s\"}######" % (
            setup["timestamp"], setup["timestamp"], setup["timestamp"])
        assert actual_raw_data == expected_data, \
            '\nActual value: \n{} \ndoes not match expected value: \n{}'.format(
                actual_raw_data, expected_data)
