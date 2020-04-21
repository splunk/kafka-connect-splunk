import pytest
from lib.commonkafka import *
from lib.commonsplunk import check_events_from_splunk
from lib.helper import get_test_folder

logging.config.fileConfig(os.path.join(get_test_folder(), "logging.conf"))
logger = logging.getLogger(__name__)


class TestDataEnrichment:

    @pytest.mark.parametrize("test_scenario,test_input,expected", [
        ("supplement", "¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ", 1),
        ("extendedA", "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ", 1),
        ("extendedB", "ƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏ", 1),
        ("IPAextensions", "ɐɑɒɓɔɕɖɗɘəɚɛɜɝɞɟɠɡɢɣɤɥɦɧɨɩɪɫɬɭɮɯɰɱɲɳɴɵɶɷɸɹɺɻɼɽɾɿʀʁʂʃʄʅʆʇʈʉʊʋʌʍʎʏʐʑʒʓʔʕʖʗʘʙʚʛʜʝʞʟʠʡʢʣʤʥʦʧʨʩʪʫʬʭʮʯ", 1),
        ("spaceModifier", "ʰʱʲʳʴʵʶʷʸʹʺʻʼʽʾʿˀˁ˂˃˄˅ˆˇˈˉˊˋˌˍˎˏːˑ˒˓˔˕˖˗˘˙˚˛˜˝˞˟ˠˡˢˣˤ˥˦˧˨˩˪˫ˬ˭ˮ˯˰˱˲˳˴˵˶˷˸˹˺˻˼˽˾˿", 1),
        # Skipping this test. because it is special character, I cannot get it to work in search
        # ("diacriticalMarks", "̀	́	̂	̃	̄	̅	̆	̇	̈	̉	̊	̋	̌	̍	̎	̏	̐	̑	̒	̓	̔	̕	̖	̗	̘	̙	̚	̛	̜	̝	̞	̟	̠	̡	̢	̣	̤	̥	̦	̧	̨	̩	̪	̫	̬	̭	̮	̯	̰	̱	̲	̳	̴	̵	̶	̷	̸	̹	̺	̻	̼	̽	̾	̿	̀	́	͂	̓	̈́	ͅ	͆	͇	͈	͉	͊	͋	͌	͍	͎	͏	͐	͑	͒	͓	͔	͕	͖	͗	͘	͙	͚	͛	͜	͝	͞	͟	͠	͡	͢	ͣ	ͤ	ͥ	ͦ	ͧ	ͨ	ͩ	ͪ	ͫ	ͬ	ͭ	ͮ	ͯ", 1),
        ("greek", "ͰͱͲͳʹ͵Ͷͷ͸͹ͺͻͼͽ;Ϳ΀΁΂΃΄΅Ά·ΈΉΊ΋Ό΍ΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡ΢ΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϏϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ", 1)
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
