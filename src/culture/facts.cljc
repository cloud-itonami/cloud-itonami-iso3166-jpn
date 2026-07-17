(ns culture.facts
  "Country-level regional-culture catalog for Japan (JPN) -- national
  dishes, protected products, beverages, crafts, festivals and heritage
  sites, per ADR-2607171400 addendum 2 (cloud-itonami-municipality-
  culture-catalog Wave 1, in com-junkawasaki/root). Sibling namespace to
  `marketentry.facts` / `statute.facts` (ADR-2607141700); city-level
  counterparts live in the cloud-itonami-municipality-* repos.

  Catalog is keyed by UPPERCASE ISO3 (mirrors `statute.facts`); entries
  carry no :culture/municipality (that attribute is city-level only).

  Every entry cites a source URL that was actually fetched and read on
  :culture/retrieved-at -- never fabricated. Summaries state only what the
  cited source confirms. An item not in this table has NO spec-basis, full
  stop; extend `catalog`, do not invent an id/url.")

(def catalog
  "iso3 -> vector of culture entries."
  {"JPN"
   [{:culture/id "jpn.dish.sushi"
     :culture/name "Sushi"
     :culture/name-local "寿司"
     :culture/country "JPN"
     :culture/kind :dish
     :culture/summary "Traditional Japanese dish of vinegared rice, typically seasoned with sugar and salt, combined with ingredients such as seafood, vegetables or meat."
     :culture/url "https://en.wikipedia.org/wiki/Sushi"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.dish.ramen"
     :culture/name "Ramen"
     :culture/name-local "ラーメン"
     :culture/country "JPN"
     :culture/kind :dish
     :culture/summary "Japanese noodle soup of Chinese-style alkaline wheat noodles in broth, a Japanese adaptation of southern Chinese noodle soups that emerged from Yokohama Chinatown around the turn of the 20th century."
     :culture/url "https://en.wikipedia.org/wiki/Ramen"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.dish.tempura"
     :culture/name "Tempura"
     :culture/name-local "天ぷら"
     :culture/country "JPN"
     :culture/kind :dish
     :culture/summary "Japanese dish of seafood and vegetables coated in a thin batter and deep-fried, originating in the 16th century when Portuguese Jesuits brought the frying technique via the Nanban trade."
     :culture/url "https://en.wikipedia.org/wiki/Tempura"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.dish.okonomiyaki"
     :culture/name "Okonomiyaki"
     :culture/name-local "お好み焼き"
     :culture/country "JPN"
     :culture/kind :dish
     :culture/summary "Japanese teppanyaki savory pancake of wheat-flour batter with cabbage and various proteins, with regional variants centered on Hiroshima and the Kansai region."
     :culture/url "https://en.wikipedia.org/wiki/Okonomiyaki"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.beverage.sake"
     :culture/name "Sake"
     :culture/name-local "日本酒"
     :culture/country "JPN"
     :culture/kind :beverage
     :culture/summary "Alcoholic beverage of Japanese origin made by fermenting polished rice, brewed by a process closer to beer than wine, typically 15-22% ABV."
     :culture/url "https://en.wikipedia.org/wiki/Sake"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.beverage.matcha"
     :culture/name "Matcha"
     :culture/name-local "抹茶"
     :culture/country "JPN"
     :culture/kind :beverage
     :culture/summary "Finely ground powder of shade-grown green tea consumed suspended in hot water and central to the Japanese tea ceremony; powdered tea originated in Tang-dynasty China, and Japan developed what is now known as matcha."
     :culture/url "https://en.wikipedia.org/wiki/Matcha"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.product.miso"
     :culture/name "Miso"
     :culture/name-local "味噌"
     :culture/country "JPN"
     :culture/kind :product
     :culture/summary "Traditional Japanese seasoning, a thick paste produced by fermenting soybeans with salt and kōji, used in soups, sauces and marinades."
     :culture/url "https://en.wikipedia.org/wiki/Miso"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.craft.arita-ware"
     :culture/name "Arita ware"
     :culture/name-local "有田焼"
     :culture/country "JPN"
     :culture/kind :craft
     :culture/summary "Japanese porcelain made around the town of Arita in former Hizen Province, Kyūshū, produced since the end of the 16th century and exported extensively to Europe in the late 1600s and early 1700s."
     :culture/url "https://en.wikipedia.org/wiki/Arita_ware"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.festival.gion-matsuri"
     :culture/name "Gion Matsuri"
     :culture/name-local "祇園祭"
     :culture/country "JPN"
     :culture/kind :festival
     :culture/summary "One of Japan's largest festivals, held annually throughout July in Kyoto and originating as a plague-purification ritual in 869; its yamaboko float processions were inscribed as UNESCO intangible cultural heritage in 2009."
     :culture/url "https://en.wikipedia.org/wiki/Gion_Matsuri"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "jpn.heritage.mount-fuji"
     :culture/name "Mount Fuji"
     :culture/name-local "富士山"
     :culture/country "JPN"
     :culture/kind :heritage
     :culture/summary "Active stratovolcano on Honshu and Japan's highest mountain at 3,776 m; added to the UNESCO World Heritage List as a cultural site in 2013."
     :culture/url "https://en.wikipedia.org/wiki/Mount_Fuji"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}]})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-jpn culture catalog "
                 "(ADR-2607171400 addendum 2, Wave 1): " (count (get catalog "JPN"))
                 " JPN entries, each with a fetched-and-read citation. "
                 "Extend `culture.facts/catalog`, never fabricate an id/url.")})))

(defn by-kind [iso3 kind]
  (filterv #(= (:culture/kind %) kind) (spec-basis iso3)))
