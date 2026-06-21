package com.example.ui.screens

import com.example.data.TouristSpot

fun translateSpotName(name: String, isEnglish: Boolean): String {
    if (!isEnglish) return name
    return when (name) {
        // Bucharest starting point
        "Gara de Nord (Hotel/Start)" -> "North Stream Station (Hotel/Start)"
        "Gara de Nord" -> "North Station"
        "Palatul Parlamentului" -> "Palace of the Parliament"
        "Centrul Vechi" -> "Old Town"
        "Ateneul Român" -> "Romanian Athenaeum"
        "Parcul Herăstrău (Mihai I)" -> "Herăstrău Park (Mihai I)"
        "Arcul de Triumf" -> "Triumph Arch"
        "Muzeul Național al Satului" -> "National Village Museum"
        "Parcul Cișmigiu" -> "Cișmigiu Gardens"
        "Muzeul Național Grigore Antipa" -> "Grigore Antipa Natural History Museum"
        "Cărturești Carusel" -> "Cărturești Carusel Bookstore"
        "Biserica Stavropoleos" -> "Stavropoleos Church"
        "Muzeul de Artă al României" -> "National Museum of Art"
        "Grădina Botanică Dimitrie Brândză" -> "Botanical Garden"
        "Parcul Carol I" -> "Carol I Park"
        "Palatul Primăverii" -> "Spring Palace"
        "Piața Revoluției" -> "Revolution Square"
        "Hanul lui Manuc" -> "Manuc's Inn"
        "Muzeul Național de Istorie a României" -> "National History Museum"
        "Palatul Cotroceni" -> "Cotroceni Palace"
        "Parcul Tineretului" -> "Tineretului Park"
        "Catedrala Mântuirii Neamului" -> "People's Salvation Cathedral"
        "Parcul Drumul Taberei" -> "Drumul Taberei Park"
        "Palatul Mogoșoaia" -> "Mogoșoaia Palace"
        "Muzeul de Artă Contemporană (MNAC)" -> "National Museum of Contemporary Art"
        "Piața Universității" -> "University Square"
        "Opera Națională București" -> "Bucharest National Opera"
        "Parcul Alexandru Ioan Cuza (IOR)" -> "Alexandru Ioan Cuza (IOR) Park"
        "Therme București" -> "Therme Bucharest"
        "Palatul Șuțu (Muzeul Bucureștiului)" -> "Sutu Palace (Bucharest History Museum)"
        "Teatrul Național I.L. Caragiale" -> "I.L. Caragiale National Theatre"
        "Observatorul Astronomic Vasile Urseanu" -> "Vasile Urseanu Astronomical Observatory"
        "Parcul Kiseleff" -> "Kiseleff Park"
        "Palatul Cantacuzino" -> "Cantacuzino Palace"
        "Pasajul Macca-Vilacrosse" -> "Macca-Vilacrosse Passage"
        "Palatul CEC" -> "CEC Palace"
        "Muzeul Colecțiilor de Artă" -> "Museum of Art Collections"
        "Palatul Justiției" -> "Palace of Justice"
        "Palatul Patriarhiei" -> "Patriarchal Palace"
        "Muzeul Militar Național" -> "National Military Museum"
        "Arena Națională" -> "National Arena"
        "Muzeul Național al Literaturii Române" -> "National Museum of Romanian Literature"
        "Palatul Kretzulescu" -> "Kretzulescu Palace"
        "Biserica Kretzulescu" -> "Kretzulescu Church"
        "Muzeul Tehnic Dimitrie Leonida" -> "Dimitrie Leonida Technical Museum"
        "Palatul Primăriei Capitalei" -> "Bucharest City Hall Palace"
        "Turnul de Artă (Pantelimon)" -> "Art Tower (Pantelimon)"
        "Muzeul Național al Hărților și Cărții Vechi" -> "National Museum of Maps and Old Books"
        "Parcul Plumbuita" -> "Plumbuita Park"
        "Parcul Circului de Stat" -> "State Circus Park"
        "Palatul Ghica Tei" -> "Ghica Tei Palace"
        "Cimitirul Bellu" -> "Bellu Cemetery"

        // Cluj starting point & presets
        "Gara Cluj-Napoca (Hotel/Start)" -> "Cluj-Napoca Railway Station (Hotel/Start)"
        "Gara Cluj-Napoca" -> "Cluj-Napoca Railway Station"
        "Grădina Botanică Alexandru Borza" -> "Botanical Garden"
        "Parcul Central Simion Bărnuțiu" -> "Central Park Simion Bărnuțiu"
        "Piața Unirii & Biserica Sf. Mihail" -> "Union Square & St. Michael Church"
        "Catedrala Mitropolitană & Piața Avram Iancu" -> "Metropolitan Cathedral & Avram Iancu Square"
        "Dealul Cetățuia" -> "Cetățuia Hill"
        "Muzeul de Artă & Palatul Bánffy" -> "Art Museum & Bánffy Palace"
        "Parcul Romulus Vuia (Etnografic)" -> "Romulus Vuia Ethnographic Park"
        "Bastionul Croitorilor" -> "Tailors' Tower"
        "Parcul Iulius (Lacul Gheorgheni)" -> "Iulius Park (Gheorgheni Lake)"
        "Piața Muzeului" -> "Museum Square"
        "Pădurea Hoia-Baciu" -> "Hoia-Baciu Haunted Forest"
        "The Office Cluj & Podul de Fier" -> "The Office Cluj & The Iron Bridge"
        "Teatrul Național și Opera Română" -> "National Theatre and Romanian Opera"
        "Turnul Pompierilor" -> "Firemen's Tower"
        "Parcul Cetățuia Buburuza" -> "Buburuza Cetățuia Park"
        "Muzeul Național de Istorie a Transilvaniei" -> "National History Museum of Transylvania"
        "Biserica Reformată de pe ulița Lupilor" -> "Reformed Church on Wolves' Street"
        "Cluj Arena" -> "Cluj Arena Stadium"
        "BT Arena (Sala Polivalentă)" -> "BT Arena Multi-purpose Hall"
        "Parcul Rozelor" -> "Rose Park"
        "Biserica Calvaria (Mănăștur)" -> "Calvaria Church (Mănăștur)"
        "Catedrala Greco-Catolică Sf. Iosif (Cipariu)" -> "St. Joseph Greek-Catholic Cathedral"
        "Observatorul Astronomic" -> "Astronomical Observatory"
        "Campusul Istoric USAMV" -> "Historical USAMV Campus"
        "Cetatea Fetei Florești" -> "Cetatea Fetei Florești (Hiking Spot)"

        // Brasov starting point & presets
        "Gara Brașov (Hotel/Start)" -> "Brașov Railway Station (Hotel/Start)"
        "Gara Brașov" -> "Brașov Railway Station"
        "Biserica Neagră" -> "The Black Church"
        "Piața Sfatului" -> "Council Square"
        "Telecabina Tâmpa" -> "Tâmpa Cable Car"
        "Turnul Alb" -> "The White Tower"
        "Poarta Șchei" -> "Șchei Gate"
        "Turnul Negru" -> "The Black Tower"
        "Bastionul Țesătorilor" -> "Weavers' Bastion"
        "Strada Sforii" -> "Rope Street"
        "Prima Școală Românească" -> "First Romanian School"
        "Poarta Ecaterinei" -> "Catherine's Gate"
        "Parcul Central Nicolae Titulescu" -> "Nicolae Titulescu Central Park"
        "Bastionul Graft" -> "Graft Bastion"
        "Muzeul de Artă Brașov" -> "Brașov Art Museum"
        "Pietrele lui Solomon" -> "Solomon's Rocks"
        "Cetățuia de pe Strajă" -> "The Citadel on Strajă Hill"
        "Turnul Măcelarilor" -> "Butchers' Tower"
        "Bastionul Cojocarilor" -> "Furriers' Bastion"
        "Sinagoga Neologă din Brașov" -> "Neologue Synagogue of Brașov"
        "Casa Sfatului (Muzeul de Istorie)" -> "Council House (History Museum)"
        "Biserica Sfântul Nicolae" -> "St. Nicholas Church"
        "Promenada de sub Tâmpa" -> "Promenade under Tâmpa Mountain"
        "Turnul Lemnarilor" -> "Woodworkers' Tower"
        "Cartierul Istoric Șchei" -> "Șchei Historical Quarter"
        "Grădina Zoologică Brașov (Noua)" -> "Brașov Zoo (Noua)"
        "Lacul Noua & Parc Agrement" -> "Noua Lake & Leisure Park"

        // Campina starting point & presets
        "Gara Câmpina (Hotel/Start)" -> "Câmpina Railway Station (Hotel/Start)"
        "Gara Câmpina" -> "Câmpina Railway Station"
        "Castelul \"Iulia Hasdeu\"" -> "Iulia Hasdeu Castle"
        "Muzeul Memorial \"Nicolae Grigorescu\"" -> "Nicolae Grigorescu Memorial Museum"
        "Casa de Cultură Câmpina" -> "Câmpina House of Culture"
        "Primăria Câmpina" -> "Câmpina City Hall"
        "Biserica de Lemn \"Adormirea Maicii Domnului\"" -> "Wood Church of Dormition"
        "Capela în stil Gotic \"Hernea\"" -> "Hernea Gothic Chapel"
        "Bulevardul Culturii (Aleea cu Platani)" -> "Culture Boulevard (Planet Avenue)"
        "Fântâna cu Cireși & Dealul Muscel" -> "Cherry Well & Muscel Hill"
        "Lacul Câmpina (Lacul Peștelui)" -> "Campina Lake (Fish Lake)"

        // Remaining cities starting points
        "Gara Sinaia (Hotel/Start)" -> "Sinaia Railway Station (Hotel/Start)"
        "Gara Sibiu (Hotel/Start)" -> "Sibiu Railway Station (Hotel/Start)"
        "Gara Sighișoara (Hotel/Start)" -> "Sighișoara Railway Station (Hotel/Start)"
        "Gara Constanța (Hotel/Start)" -> "Constanța Railway Station (Hotel/Start)"
        "Gara Iași (Hotel/Start)" -> "Iași Railway Station (Hotel/Start)"
        "Gara Timișoara (Hotel/Start)" -> "Timișoara Railway Station (Hotel/Start)"
        "Gara Oradea (Hotel/Start)" -> "Oradea Railway Station (Hotel/Start)"
        "Gara Alba Iulia (Hotel/Start)" -> "Alba Iulia Railway Station (Hotel/Start)"
        "Gara Suceava (Hotel/Start)" -> "Suceava Railway Station (Hotel/Start)"
        "Gara Craiova (Hotel/Start)" -> "Craiova Railway Station (Hotel/Start)"
        "Gara Arad (Hotel/Start)" -> "Arad Railway Station (Hotel/Start)"
        "Gara Galați (Hotel/Start)" -> "Galați Railway Station (Hotel/Start)"
        "Gara Târgu Mureș (Hotel/Start)" -> "Târgu Mureș Railway Station (Hotel/Start)"
        "Gara Satu Mare (Hotel/Start)" -> "Satu Mare Railway Station (Hotel/Start)"
        "Gara Bacău (Hotel/Start)" -> "Bacău Railway Station (Hotel/Start)"
        "Gara Ploiești Sud (Hotel/Start)" -> "Ploiești South Railway Station (Hotel/Start)"
        "Gara Miercurea Ciuc (Hotel/Start)" -> "Miercurea Ciuc Railway Station (Hotel/Start)"
        "Gara Pitești Sud (Hotel/Start)" -> "Pitești South Railway Station (Hotel/Start)"
        "Gara Brăila (Hotel/Start)" -> "Brăila Railway Station (Hotel/Start)"
        "Gara Baia Mare (Hotel/Start)" -> "Baia Mare Railway Station (Hotel/Start)"
        "Gara Bistrița Nord (Hotel/Start)" -> "Bistrița North Railway Station (Hotel/Start)"
        "Gara Târgoviște (Hotel/Start)" -> "Târgoviște Railway Station (Hotel/Start)"
        "Gara Tulcea (Hotel/Start)" -> "Tulcea Railway Station (Hotel/Start)"
        "Gara Piatra Neamț (Hotel/Start)" -> "Piatra Neamț Railway Station (Hotel/Start)"
        "Gara Râmnicu Vâlcea (Hotel/Start)" -> "Râmnicu Vâlcea Railway Station (Hotel/Start)"
        "Gara Drobeta-Turnu Severin (Hotel/Start)" -> "Drobeta-Turnu Severin Railway Station (Hotel/Start)"
        "Gara Deva (Hotel/Start)" -> "Deva Railway Station (Hotel/Start)"
        "Gara Botoșani (Hotel/Start)" -> "Botoșani Railway Station (Hotel/Start)"
        "Gara Sfântu Gheorghe (Hotel/Start)" -> "Sfântu Gheorghe Railway Station (Hotel/Start)"
        "Gara Giurgiu Oraș (Hotel/Start)" -> "Giurgiu City Railway Station (Hotel/Start)"
        "Gara Călărași Sud (Hotel/Start)" -> "Călărași South Railway Station (Hotel/Start)"
        "Gara Slobozia Veche (Hotel/Start)" -> "Slobozia Veche Railway Station (Hotel/Start)"
        "Gara Zalău Nord (Hotel/Start)" -> "Zalău North Railway Station (Hotel/Start)"
        "Gara Focșani (Hotel/Start)" -> "Focșani Railway Station (Hotel/Start)"
        "Gara Buzău (Hotel/Start)" -> "Buzău Railway Station (Hotel/Start)"
        "Gara Reșița Sud (Hotel/Start)" -> "Reșița South Railway Station (Hotel/Start)"
        "Gara Târgu Jiu (Hotel/Start)" -> "Târgu Jiu Railway Station (Hotel/Start)"
        "Gara Slatina (Hotel/Start)" -> "Slatina Railway Station (Hotel/Start)"
        "Gara Alexandria (Hotel/Start)" -> "Alexandria Railway Station (Hotel/Start)"
        "Gara Vaslui (Hotel/Start)" -> "Vaslui Railway Station (Hotel/Start)"
        "Gara Hunedoara (Hotel/Start)" -> "Hunedoara Railway Station (Hotel/Start)"
        "Gara Turda (Hotel/Start)" -> "Turda Railway Station (Hotel/Start)"
        "Gara Mangalia (Hotel/Start)" -> "Mangalia Railway Station (Hotel/Start)"
        "Gara Bușteni (Hotel/Start)" -> "Bușteni Railway Station (Hotel/Start)"
        "Gara Regală Curtea de Argeș (Hotel/Start)" -> "Curtea de Argeș Royal Railway Station (Hotel/Start)"
        "Gara Gura Humorului (Hotel/Start)" -> "Gura Humorului Railway Station (Hotel/Start)"
        "Gara Vatra Dornei (Hotel/Start)" -> "Vatra Dornei Railway Station (Hotel/Start)"
        "Gara Sovata (Hotel/Start)" -> "Sovata Railway Station (Hotel/Start)"
        "Gara Băile Felix (Hotel/Start)" -> "Băile Felix Railway Station (Hotel/Start)"
        "Gara Slănic Moldova (Hotel/Start)" -> "Slănic Moldova Railway Station (Hotel/Start)"

        "Gara Băile Herculane (Hotel/Start)" -> "Băile Herculane Railway Station (Hotel/Start)"
        "Gara Călimănești (Hotel/Start)" -> "Călimănești Railway Station (Hotel/Start)"
        "Gara Borsec (Hotel/Start)" -> "Borsec Railway Station (Hotel/Start)"
        "Gara Băile Govora (Hotel/Start)" -> "Băile Govora Railway Station (Hotel/Start)"
        "Gara Câmpulung Moldovenesc (Hotel/Start)" -> "Câmpulung Moldovenesc Railway Station (Hotel/Start)"

        else -> name
    }
}

fun translateSpotDescription(description: String, isEnglish: Boolean): String {
    if (!isEnglish) return description
    return when (description) {
        "Punctul de pornire al călătoriei." -> "The starting point of your custom tour."
        "Una dintre cele mai mari clădiri administrative din lume." -> "One of the largest administrative buildings in the world."
        "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă." -> "The historical heart of Bucharest, bursting with life and old buildings."
        "O bijuterie arhitecturală de importanță istorică națională." -> "An architectural treasure of national historical importance."
        "Un parc uriaș, liniștit, situat în jurul unui lac superb." -> "A massive, peaceful park arranged around a pristine lake."
        "Monumentul care celebrează victoria României în Primul Război Mondial." -> "The monument celebrating Romania's victory in World War I."
        "O incursiune în viața rurală tradițională românească în aer liber." -> "An open-air museum exploration of traditional Romanian village life."
        "Cea mai veche grădină publică din București, un lac romantic și alei liniștite." -> "The oldest public garden in Bucharest, featuring a romantic lake and quiet alleys."
        "Expoziții interactive de zoologie, biodiversitate și fosile de dinozaur." -> "Interactive exhibitions of zoology, biodiversity, and dinosaur fossils."
        "Una dintre cele mai spectaculoase librării din lume, situată în Centrul Vechi." -> "One of the most spectacular bookshops in the world, in the Old Town."
        "O capodoperă a stilului brâncovenesc, faimoasă pentru curtea sa interioară." -> "A masterpiece of Brâncovenesc style, famous for its interior courtyard."
        "Fostul Palat Regal găzduiește colecții remarcabile de artă românească." -> "The former Royal Palace, hosting remarkable collections of Romanian art."
        "Oaze de verdeață, sere exotice tropicale și mii de specii de plante în Cotroceni." -> "A green oasis with exotic tropical greenhouses and thousands of plant species in Cotroceni."
        "Parc istoric frumos cu Mausoleul impunător și fântâni elegante." -> "Beautiful historical park with a majestic Mausoleum and elegant fountains."

        // Campina descriptions
        "Un castel încărcat de mister, construit de savantul Bogdan Petriceicu Hasdeu în memoria fiicei sale geniale, Iulia." -> "A mysterious castle built by B.P. Hasdeu in memory of his genius daughter, Iulia."
        "Casa memorială unde marele pictor Nicolae Grigorescu a trăit și creat în ultimii săi ani de viață." -> "The memorial house where the great painter Nicolae Grigorescu lived and created in his twilight years."
        "Centrul cultural principal al orașului, gazdă a numeroase spectacole, expoziții și evenimente locale." -> "The city's main cultural center, hosting numerous performances, exhibitions, and local events."
        "Clădirea administrativă centrală din Câmpina, situată pe pitorescul Bulevard al Culturii." -> "The central administrative building of Campina, located on the scenic Culture Boulevard."
        "O pitorească biserică istorică de lemn datând de la 1714, formată dintr-un singur trunchi de stejar." -> "A picturesque historic wooden church dating to 1714, carved out of a single massive oak trunk."
        "O capelă gotică misterioasă, monument de arhitectură, ridicată în memoria pionierului petrolului, Dumitru Hernea." -> "A mysterious Gothic chapel built in memory of petroleum pioneer Dumitru Hernea."
        "Zonă de promenadă superbă și relaxantă mărginită de platani uriași, considerat unul dintre cele mai ozonate locuri." -> "A gorgeous promenade flanked by towering sycamores, considered one of the highest ozone spots in Europe."
        "Cel mai înalt punct de belvedere din zonă, oferind panorame uluitoare spre valea Doftanei și dealurile prahovene." -> "Highest local scenic outlook, offering stunning panoramas of Doftana Valley and the Prahova hills."
        "Un lac natural liniștit ideal pentru plimbări relaxante pe mal, pescuit și evadare în mijlocul naturii locale." -> "A serene natural lake perfect for calm boardwalk strolls, recreation and local nature getaways."
        "Fostul palat luxos de protocol al soților Nicolae și Elena Ceaușescu." -> "The former luxurious private residence of Nicolae and Elena Ceaușescu."
        "Piața istorică centrală cu Memorialul Renașterii și clădiri celebre." -> "The central historical square with the Memorial of Rebirth."
        "Cel mai vechi han funcțional din Europa, oferind o ambianță tradițională excelentă." -> "The oldest active inn in Europe, offering an excellent traditional Romanian vibe."
        "Exponate arheologice și istorice inestimabile, incluzând Tezaurul istoric național." -> "Invaluable archaeological and historical exhibits, including the national Treasury."
        "Reședința oficială a Președintelui și un muzeu istoric de o rară frumusețe." -> "The official Presidential residence and a historic museum of rare beauty."
        "Un parc modern imens cu lac de agrement, piste și un ambient extrem de relaxant." -> "A massive modern park with a recreational lake, tracks, and relaxing ambiance."
        "Cea mai mare catedrală ortodoxă din lume, o structură arhitecturală colosală." -> "The largest Orthodox Cathedral in the world, a colossal architectural masterwork."
        "Cunoscut și ca Parcul Moghioroș, revitalizat cu poduri cochete și sere moderne." -> "Also known as Moghioroș Park, revitalized with chic bridges and modern greenhouses."
        "O clădire istorică în stil brâncovenesc deosebit, situată în exteriorul orașului." -> "A beautiful brâncovenesc-style castle situated just outside the city."
        "Situat în aripa din spate a Palatului Parlamentului, cu expoziții avangardiste." -> "Located in the back wing of the Palace of the Parliament, featuring avant-garde exhibitions."
        "Kilometrul zero al democrației bucureștene, încadrat de clădiri universitare emblematice." -> "The landmark of Romanian democracy, surrounded by iconic university buildings."
        "Clădire istorică neoclasică, faimos centru de cultura pentru spectacole lirice și balet." -> "A historic neoclassical building, a famous cultural venue for opera and ballet."

        // Cluj descriptions
        "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză." -> "A magnificent green oasis sheltering rare plants and a Japanese garden."
        "Parcul istoric central cu un lac superb de plimbări cu barca și Casino." -> "The historic central park with a boating lake and the Casino building."
        "Piața istorică principală delimitată de monumentala catedrală gotică." -> "The main historical square dominated by the monumental Gothic Cathedral."
        "Catedrală ortodoxă impunătoare și piațetă cu fântâni arteziene animate." -> "An imposing Orthodox Cathedral and public square with animated artesian fountains."
        "O panoramă spectaculaosă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
        "O panoramă spectaculoasă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
        "Palat baroc splendid ce găzduiește colecții naționale valoroase de artă." -> "A splendid baroque palace hosting valuable national art collections."
        "Primul muzeu în aer liber din România cu gospodării tradiționale transilvănene." -> "Romania's first open-air museum featuring historic Transylvanian homesteads."
        "Unul dintre puțele turnuri de apărare care s-au păstrat intacte din vechea cetate." -> "One of the few defensive towers preserved intact from the old citadel."
        "Zonă modernă de recreere în jurul lacului, plină de spații verzi și pontoane." -> "A modern lakeside recreation area filled with green spaces and boardwalks."
        "Cea mai veche piață din Cluj-Napoca, flancată de Biserica Franciscană." -> "The oldest square in Cluj-Napoca, flanked by the elegant Franciscan Church."
        "Pădurea faimoasă la nivel mondial pentru peisajele sale misterioase și legende." -> "The world-famous forest known for its mysterious landscapes and legends."
        "O zonă modernă vibrantă, îmbinând arhitectura de birouri cu malul Someșului." -> "A vibrant modern area combining office development with the Someș riverfront."
        "Clădire neobarocă superbă destinată spectacolelor lirice și teatrale." -> "A superb neo-baroque building designed for opera and theatrical performances."
        "Turn istoric reabilitat recent cu o platformă panoramică superbă." -> "A newly rehabilitated historical tower with a magnificent panoramic platform."
        "Zonă adiacentă cetățuii cu alei umbroase, spații de joacă și belvedere retras." -> "Area near Cetățuia with shaded alleys, playgrounds, and cozy viewpoints."
        "Colecții arheologice valoroase despre istoria antică, romană și medievală a Transilvaniei." -> "Valuable archaeological collections about the ancient, Roman, and medieval history of Transylvania."
        "O clădire monument istoric gotic de tip sală, una dintre cele mai vaste din Europa de Est." -> "A historic monumental Gothic hall church, one of the largest in Eastern Europe."
        "Cel mai modern stadion multifuncțional din inima Transilvaniei, cu o arhitectură high-tech." -> "The most modern multi-use stadium in the heart of Transylvania, featuring high-tech architecture."
        "Cea mai mare sală polivalentă din România, găzduiește concerte mari și evenimente sportive." -> "The largest multi-purpose arena in Romania, hosting major concerts and sports events."
        "Parc renumit pentru sutele de soiuri de trandafiri și faleza liniștită pe malul Someșului." -> "A park famous for hundreds of rose varieties and a peaceful Someș riverfront path."
        "O veche mănăstire benedictină fortificată, fiind una dintre cele mai bătrâne biserici din Cluj." -> "An ancient fortified Benedictine monastery, one of the oldest standing churches in Cluj."
        "Catedrală monumentală cu design modern magnific, aflată în Piața Cipariu." -> "A monumental cathedral with a magnificent modern design, located in Cipariu Square."
        "Situat în campusul USAMV, ideal pentru explorarea stelelor și activități educaționale." -> "Located on the USAMV campus, ideal for star-gazing and educational outreach."
        "Grădini, livezi istorice și oază verde extinsă în una dintre faimoasele universități clujene." -> "Gardens, orchards, and an expansive green sanctuary inside USAMV University."
        "Loc istoric plin de mister situat pe deal, înconjurat de pădure, ideal pentru drumeții." -> "A mysterious historical site situated on a hill, surrounded by forest, ideal for hiking."

        // Brasov descriptions
        "Cea mai mare biserică gotică din sud-estul Europei." -> "The largest Gothic church in Southeastern Europe."
        "Piața istorică principală din Brașov, plină de farmec și cafenele." -> "The main historical square in Brașov, filled with charm and cozy cafes."
        "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului." -> "The cable car climbing Tâmpa Mountain, offering scenic panoramic city views."
        "Turn istoric de apărare oferind o vedere spectaculoasă la înălțime." -> "A historic defense tower providing spectacular aerial views from the hill."
        "Poartă barocă superbă ce duce spre vechiul cartier românesc." -> "A beautiful baroque gate leading into the historic Romanian quarter Schei."
        "Turn de strajă din secolul al XV-lea cu vedere panoramică spre Biserica Neagră." -> "A 15th-century defense tower with panoramic views looking towards the Black Church."
        "Unul dintre cele mai bine conservate bastioane medievale, adăpostind o machetă rară." -> "One of the best-preserved medieval bastions, hosting a rare scale model."
        "Una dintre cele mai înguste străzi din Europa, un reper fotografic iconic." -> "One of the narrowest alleys in Europe, a truly iconic photo landmark."
        "Situată în Șchei, locul unde s-au tipărit primele cărți în limba română." -> "Located in Șchei, the historic cradle where the first Romanian books were printed."
        "Singura poartă medievală de acces în cetate păstrată în forma sa originală." -> "The only medieval city entry gate surviving fully in its original form."
        "Un park mare și liniștit în centrul orașului cu alei largi și fântâni." -> "A large and tranquil park in the city center with wide paths and fountains."
        "Bastion fortificat pitoresc deasupra pârâului Graft, legat de Turnul Alb." -> "A picturesque fortified bastion over Graft creek, linked physically to the White Tower."
        "Expoziție de pictură și sculptură românească valoroasă, aproape de primărie." -> "A rich collection of valuable Romanian paintings and sculptures near City Hall."
        "Zonă naturală de chei spectaculoase cu spații verzi pentru recreere." -> "A spectacular natural gorge area with lush green spaces for picnics."
        "Fortăreață istorică pe dealul Strajă, monument istoric de importanță națională." -> "A hilltop citadel on Strajă hill, a national historical heritage site."
        "Turn vechi de apărare din secolul al XV-lea, parte integrantă din fortificații." -> "An ancient 15th-century defense tower, integral to the old town walls."
        "Bastion istoric ridicat pe latura de sud a cetății sub muntele Tâmpa." -> "A historic bastion erected on the southern walls right under Tâmpa Mountain."
        "O clădire religioasă splendidă în stil bizantin, cu detalii decorative fermecătoare." -> "A splendid Religious monument designed in Byzantine style with charming decor."
        "Simbolul central al orașului Brașov, fostul sediu administrativ medieval." -> "The central symbol of Brașov, formerly the medieval administrative headquarters."
        "O biserică orthodoxă impunătoare din Șchei, fondată în secolul al XIII-lea." -> "An imposing Orthodox Church in Șchei, with foundations from the 13th century."
        "Aleea pietonală umbroasă Tiberiu Brediceanu, perfectă pentru plimbări relaxante pe sub pădure." -> "The shaded pedestrian alley under Tâmpa, perfect for relaxing forest walks."
        "Turnul Lemnarilor" -> "The Woodworkers' Tower"
        "Turnul Lemnarilor, găzduiește expoziții de sculptură și ateliere de artă." -> "The Woodworkers' Tower, showcasing woodcarving exhibits and art workshops."
        "Turn istoric restaurat cochet, găzduiește expoziții de sculptură și ateliere de artă." -> "A beautifully restored historic tower, hosting sculpture exhibits and art workshops."
        "Explorare pe străduțele vechi și întortocheate, inima spiritului românesc brașovean." -> "An exploration of winding old cobblestone streets, the cradle of Romanian heritage."
        "Una dintre cele mai moderne grădini zoologice din țară, amplasată în pădurea Noua." -> "One of the country's most modern zoos, nestling beautifully in Noua Forest."
        "Zonă superbă de relaxare cu bărci, pontoane, terenuri de sport și un aer minunat de munte." -> "A stellar lakeside park with rental boats, sports grounds, and pure mountain air."
        else -> description
    }
}

fun TouristSpot.translate(isEnglish: Boolean): TouristSpot {
    return this.copy(
        name = translateSpotName(this.name, isEnglish),
        description = translateSpotDescription(this.description, isEnglish)
    )
}

fun getSpotCategoryEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("observator") || lower.contains("astronomic") -> "🔭"
        lower.contains("therme") || lower.contains("wellness") -> "🌴"
        lower.contains("stadion") || lower.contains("arena națională") || lower.contains("cluj arena") || lower.contains("bt arena") -> "🏟️"
        lower.contains("librărie") || lower.contains("cărturești") -> "📖"
        lower.contains("teatru") || lower.contains("operă") || lower.contains("ateneu") || lower.contains("spectacol") || lower.contains("filarmonică") -> "🎭"
        lower.contains("cimitir") -> "🪦"
        lower.contains("pasaj") -> "🏮"
        lower.contains("parc") || lower.contains("grădină") || lower.contains("bulevard") || lower.contains("lac") || lower.contains("cismigiu") || lower.contains("herăstrău") -> "🌳"
        lower.contains("muzeu") || lower.contains("artă") || lower.contains("istorie") || lower.contains("antipa") || lower.contains("antichități") || lower.contains("cultur") || lower.contains("literatur") -> "🏛️"
        lower.contains("biserică") || lower.contains("catedrală") || lower.contains("mitropolie") || lower.contains("biserica") || lower.contains("monastir") || lower.contains("sinagogă") || lower.contains("templu") || lower.contains("patriarh") -> "⛪"
        lower.contains("castel") || lower.contains("palat") || lower.contains("cetatea") || lower.contains("cetate") || lower.contains("bastion") || lower.contains("turn") -> "🏰"
        lower.contains("mall") || lower.contains("unirea") || lower.contains("afi") || lower.contains("bazar") || lower.contains("piața") || lower.contains("comercial") || lower.contains("plazza") -> "🛍️"
        lower.contains("restaurant") || lower.contains("cafenea") || lower.contains("ceainărie") || lower.contains("pub") || lower.contains("bere") -> "🍽️"
        lower.contains("grădina zoologică") || lower.contains("zoo") || lower.contains("animale") -> "🦁"
        else -> "📍"
    }
}

fun translateVehicleType(type: String): String {
    val t = type.lowercase().trim()
    return when {
        t.contains("metrou") -> "Metro"
        t.contains("autobuz") || t.contains("bus") -> "Bus"
        t.contains("troleibuz") || t.contains("trolley") -> "Trolleybus"
        t.contains("tramvai") || t.contains("tram") -> "Tram"
        else -> type
    }
}

fun translateDirections(directions: String, isEnglish: Boolean): String {
    if (!isEnglish) return directions

    // 1. Ridesharing / Taxi template
    val taxiRegex = """Ia un Ridesharing / Taxi de la (.*) până la (.*) • Tarif estimat Uber/Taxi: (.*) Lei \(Distanță: (.*) m, ~(.*) min\)\.""".toRegex()
    taxiRegex.find(directions)?.let { match ->
        val from = match.groupValues[1]
        val to = match.groupValues[2]
        val fare = match.groupValues[3]
        val distance = match.groupValues[4]
        val duration = match.groupValues[5]
        return "Take a Rideshare / Taxi from ${translateSpotName(from, true)} to ${translateSpotName(to, true)} • Estimated fee: $fare Lei (Distance: $distance m, ~$duration min)."
    }

    // 2. Walk to with from/to template
    val walkFromToRegex = """Mergi pe jos (.*) m \(~(.*) min\) de la (.*) până la (.*)\.""".toRegex()
    walkFromToRegex.find(directions)?.let { match ->
        val distance = match.groupValues[1]
        val duration = match.groupValues[2]
        val from = match.groupValues[3]
        val to = match.groupValues[4]
        return "Walk $distance m (~$duration min) from ${translateSpotName(from, true)} to ${translateSpotName(to, true)}."
    }

    // 3. Walk to template
    val walkToRegex = """Mergi pe jos (.*) m \(~(.*) min\) până la (.*)\.""".toRegex()
    walkToRegex.find(directions)?.let { match ->
        val distance = match.groupValues[1]
        val duration = match.groupValues[2]
        val to = match.groupValues[3]
        return "Walk $distance m (~$duration min) to ${translateSpotName(to, true)}."
    }

    // 4. Walk to station template
    val walkStationRegex = """Mergi pe jos (.*) m până la stația: (.*)\.""".toRegex()
    walkStationRegex.find(directions)?.let { match ->
        val distance = match.groupValues[1]
        val station = match.groupValues[2]
        return "Walk $distance m to station: ${translateSpotName(station, true)}."
    }

    // 5. Board at, ride stations and exit at template
    val boardRegex = """Urcă în (.*) (.*) la (.*), mergi (.*) stații și coboară la (.*)\.""".toRegex()
    boardRegex.find(directions)?.let { match ->
        val type = match.groupValues[1]
        val line = match.groupValues[2]
        val station = match.groupValues[3]
        val stationsCount = match.groupValues[4]
        val dest = match.groupValues[5]
        val engType = translateVehicleType(type)
        return "Board $engType $line at ${translateSpotName(station, true)}, ride $stationsCount stations and exit at ${translateSpotName(dest, true)}."
    }

    // 6. Transfer template
    val transferRegex = """Schimbă mijlocul de transport în stația (.*) \(~(.*) min\)\.""".toRegex()
    transferRegex.find(directions)?.let { match ->
        val station = match.groupValues[1]
        val minutes = match.groupValues[2]
        return "Transfer to another transit line at station ${translateSpotName(station, true)} (~$minutes min)."
    }

    // 7. Board and travel template
    val boardTravelRegex = """Urcă în (.*) (.*) și călătorește (.*) stații până la (.*)\.""".toRegex()
    boardTravelRegex.find(directions)?.let { match ->
        val type = match.groupValues[1]
        val line = match.groupValues[2]
        val stationsCount = match.groupValues[3]
        val dest = match.groupValues[4]
        val engType = translateVehicleType(type)
        return "Board $engType $line and ride $stationsCount stations to ${translateSpotName(dest, true)}."
    }

    // 8. Ride from station to station template
    val rideTemplateRegex = """Călătorește cu (.*) (.*) din stația (.*) până la stația (.*) \((.*) stații\)\.""".toRegex()
    rideTemplateRegex.find(directions)?.let { match ->
        val type = match.groupValues[1]
        val line = match.groupValues[2]
        val station = match.groupValues[3]
        val dest = match.groupValues[4]
        val stationsCount = match.groupValues[5]
        val engType = translateVehicleType(type)
        return "Ride $engType $line from station ${translateSpotName(station, true)} to station ${translateSpotName(dest, true)} ($stationsCount stations)."
    }

    // 9. From station walk template
    val fromStationWalkRegex = """De la stația (.*), mergi pe jos (.*) m până la (.*)\.""".toRegex()
    fromStationWalkRegex.find(directions)?.let { match ->
        val station = match.groupValues[1]
        val distance = match.groupValues[2]
        val dest = match.groupValues[3]
        return "From station ${translateSpotName(station, true)}, walk $distance m to ${translateSpotName(dest, true)}."
    }

    // Fallback to sequential replacements for any remaining phrases
    return directions
        .replace("Plimbare pe jos", "Walk")
        .replace("plimbare pe jos", "walk")
        .replace("plimbare", "walk")
        .replace("pe jos", "on foot")
        .replace("până la", "to")
        .replace("către", "towards")
        .replace("de la", "from")
        .replace("la", "at")
        .replace("stânga", "left")
        .replace("dreapta", "right")
        .replace("mergi", "go")
        .replace("Mergi", "Go")
        .replace("Ia autobuzul", "Take bus")
        .replace("Ia troleibuzul", "Take trolley")
        .replace("Ia tramvaiul", "Take tram")
        .replace("Ieși din", "Exit from")
        .replace("stația", "station")
        .replace("Stația", "Station")
        .replace("direcția", "direction")
        .replace("coboară la", "get off at")
        .replace("Coboară la", "Get off at")
        .replace("spre", "towards")
        .replace("Spre", "Towards")
        .replace("apoi mergi în jur de", "then walk about")
        .replace("până la intrare", "to the entrance")
        .replace("metrou", "metro")
        .replace("Metrou", "Metro")
        .replace("linii", "lines")
        .replace("linia", "line")
        .replace("Autobuz", "Bus")
        .replace("autobuz", "bus")
        .replace("Tramvai", "Tram")
        .replace("tramvai", "tram")
        .replace("Troleibuz", "Trolleybus")
        .replace("troleibuz", "trolleybus")
        .replace("Timp estimat", "Estimated time")
        .replace("minute", "minutes")
        .replace("minut", "minute")
}

fun translateAlertText(text: String, isEnglish: Boolean): String {
    if (!isEnglish) return text
    return when (text) {
        "Lucrări pe Linia 41" -> "Works on Line 41"
        "Tramvaiele liniei 41 circulă deviat temporar din cauza lucrărilor de reabilitare a carosabilului din zona Pasajului Grant. Autobuzele navetă 641 preiau fluxul de călători." ->
            "Tram 41 is temporarily rerouted due to roadworks near Grant Passage. Shuttle buses 641 are covering the passenger flow."
        "Modificare Traseu Eveniment" -> "Event Route Changes"
        "În weekendul curent, liniile din zona Calea Victoriei sunt deviate temporar pentru evenimentul Străzi Deschise." ->
            "This weekend, bus lines around Calea Victoriei are temporarily rerouted for the 'Străzi Deschise' event."
        "Zilele Clujului - Trasee Deviate" -> "Zilele Clujului - Rerouted Lines"
        "Liniile de troleibuz 6, 7 și 25 vor avea traseul scurtat până în Piața Cipariu în intervalul orar 18:00 - 23:00 pentru concertele din Piața Unirii." ->
            "Trolleybus lines 6, 7, and 25 will be shortened to Cipariu Square from 18:00 to 23:00 due to concerts in Piața Unirii."
        "Suplimentare Linie 20 Poiana" -> "Poiana Line 20 Supplemental Buses"
        "A fost suplimentat numărul de autobuze de pe linia 20 Livada Poștei - Poiana Brașov datorită afluxului masiv de turiști sosiți în weekend." ->
            "Additional buses have been scheduled on Line 20 (Livada Poștei - Poiana Brașov) due to massive tourist influx over the weekend."
        else -> text
    }
}

fun getTranslatedCostExplanation(city: String, isEnglish: Boolean, defaultExplanation: String): String {
    if (!isEnglish) return defaultExplanation
    return when (city) {
        "București" -> "1.30 € + VAT (~6.5 Lei) for a 90-minute urban travel on any metropolitan STB line."
        "Brașov" -> "4.00 Lei for a 60-minute travel on the urban RATBV network."
        "Câmpina" -> "3.00 Lei for a single travel on the urban Eliado Câmpina network."
        "Cluj-Napoca" -> "0.65 € + VAT (~3.2 Lei) for a 30-minute urban travel on any CTP Cluj line."
        else -> "Standard local fare for a single public transit journey in $city."
    }
}

fun translateCityName(city: String, isEnglish: Boolean): String {
    return if (isEnglish) {
        when (city) {
            "București" -> "Bucharest"
            "Brașov" -> "Brasov"
            "Câmpina" -> "Campina"
            else -> city
        }
    } else {
        city
    }
}

fun translateRouteDetailsText(text: String, isEnglish: Boolean): String {
    if (!isEnglish) return text
    var translated = text
        .replace("Pornire:", "Start:")
        .replace("Gara Centrală", "Central Station")
        .replace("Traseu:", "Route:")
        .replace("Etape de transport:", "Transport stages:")
    val lines = translated.split("\n").map { line ->
        if (line.trim().startsWith("- ")) {
            val rawStage = line.trim().substring(2) // remove "- "
            val durationIndex = rawStage.lastIndexOf("(")
            if (durationIndex != -1) {
                val directions = rawStage.substring(0, durationIndex).trim()
                val durationPart = rawStage.substring(durationIndex).trim() // e.g. "(15 min)"
                val translatedDurationPart = durationPart.replace("min", "minutes")
                "- ${translateDirections(directions, true)} $translatedDurationPart"
            } else {
                "- ${translateDirections(rawStage, true)}"
            }
        } else {
            if (line.startsWith("Route: ")) {
                val routeList = line.substring(7)
                val translatedRouteList = routeList.split(" ➔ ").map { spot ->
                    translateSpotName(spot.trim(), true)
                }.joinToString(" ➔ ")
                "Route: $translatedRouteList"
            } else if (line.startsWith("Start: ")) {
                val startName = line.substring(7).trim()
                "Start: ${translateSpotName(startName, true)}"
            } else {
                line
            }
        }
    }
    return lines.joinToString("\n")
}
