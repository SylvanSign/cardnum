(ns test.cards.operations
  (:require [game.core :as core]
            [test.core :refer :all]
            [test.utils :refer :all]
            [test.macros :refer :all]
            [clojure.test :refer :all]))

(deftest twenty-four-seven-news-cycle-breaking-news
  ;; 24/7 News Cycle - Breaking News interaction
  (do-game
    (new-game (default-corp [(qty "Breaking News" 2) (qty "24/7 News Cycle" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Breaking News" "New remote")
    (play-from-hand state :resPlayer "Breaking News" "New remote")
    (let [ag1 (get-content state :remote1 0)
          ag2 (get-content state :remote2 0)]
      (score-agenda state :resPlayer ag1)
      (score-agenda state :resPlayer ag2)
      (take-credits state :resPlayer)
      (is (= 0 (:tag (get-runner)))) ; tags cleared
      (take-credits state :hazPlayer)
      (play-from-hand state :resPlayer "24/7 News Cycle")
      (prompt-select :resPlayer (find-card "Breaking News" (:scored (get-corp))))
      (is (= 1 (:agenda-point (get-corp))) "Forfeited Breaking News")
      (prompt-select :resPlayer (find-card "Breaking News" (:scored (get-corp))))
      (is (= 2 (:tag (get-runner))) "Runner given 2 tags")
      (take-credits state :resPlayer 2)
      (is (= 2 (:tag (get-runner))) "Tags remained after Corp ended turn"))))

(deftest twenty-four-seven-news-cycle-posted-bounty
  ;; 24/7 News Cycle and Posted Bounty interaction -- Issue #1043
  (do-game
    (new-game (default-corp [(qty "Posted Bounty" 2) (qty "24/7 News Cycle" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Posted Bounty" "New remote")
    (play-from-hand state :resPlayer "Posted Bounty" "New remote")
    (let [ag1 (get-content state :remote1 0)
          ag2 (get-content state :remote2 0)]
      (score-agenda state :resPlayer ag1)
      (prompt-choice :resPlayer "No")
      (score-agenda state :resPlayer ag2)
      (prompt-choice :resPlayer "No")
      (play-from-hand state :resPlayer "24/7 News Cycle")
      (prompt-select :resPlayer (find-card "Posted Bounty" (:scored (get-corp))))
      (is (= 1 (:agenda-point (get-corp))) "Forfeited Posted Bounty")
      (prompt-select :resPlayer (find-card "Posted Bounty" (:scored (get-corp))))
      (prompt-choice :resPlayer "Yes") ; "Forfeit Posted Bounty to give 1 tag?"
      (is (= 1 (:tag (get-runner))) "Runner given 1 tag")
      (is (= 1 (:bad-publicity (get-corp))) "Corp has 1 bad publicity")
      (is (= 0 (:agenda-point (get-corp))) "Forfeited Posted Bounty to 24/7 News Cycle"))))

(deftest twenty-four-seven-news-cycle-swaps
  ;; 24/7 News Cycle - Swapped agendas are able to be used. #1555
  (do-game
    (new-game (default-corp [(qty "24/7 News Cycle" 1) (qty "Chronos Project" 1)
                             (qty "Philotic Entanglement" 1) (qty "Profiteering" 1)])
              (default-runner [(qty "Turntable" 3)]))
    (score-agenda state :resPlayer (find-card "Chronos Project" (:hand (get-corp))))
    (score-agenda state :resPlayer (find-card "Philotic Entanglement" (:hand (get-corp))))
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Turntable")
    (core/steal state :hazPlayer (find-card "Profiteering" (:hand (get-corp))))
    (prompt-choice :hazPlayer "Yes")
    (prompt-select :hazPlayer (find-card "Philotic Entanglement" (:scored (get-corp))))
    (is (= 2 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "24/7 News Cycle")
    (prompt-select :resPlayer (find-card "Chronos Project" (:scored (get-corp))))
    (is (= "Chronos Project" (:title (first (:rfg (get-corp))))))
    ;; shouldn't work on an agenda in the Runner's scored area
    (is (= 2 (count (:hand (get-runner)))))
    (prompt-select :resPlayer (find-card "Philotic Entanglement" (:scored (get-runner))))
    (is (= 2 (count (:hand (get-runner)))))
    ;; resolve 'when scored' ability on swapped Profiteering
    (is (= 8 (:credit (get-corp))))
    (prompt-select :resPlayer (find-card "Profiteering" (:scored (get-corp))))
    (prompt-choice :resPlayer "3")
    (is (= 1 (:agenda-point (get-corp))))
    (is (= 3 (:bad-publicity (get-corp))))
    (is (= 23 (:credit (get-corp))) "Gained 15 credits")))

(deftest accelerated-diagnostics
  ;; Accelerated Diagnostics - Interaction with prompt effects, like Shipment from SanSan
  (do-game
    (new-game (default-corp [(qty "Accelerated Diagnostics" 1) (qty "Cerebral Overwriter" 1) (qty "Shipment from SanSan" 1)
                             (qty "Hedge Fund" 1) (qty "Back Channels" 1)])
              (default-runner))
    (starting-hand state :resPlayer ["Accelerated Diagnostics" "Cerebral Overwriter"])
    (play-from-hand state :resPlayer "Cerebral Overwriter" "New remote")
    (core/gain state :resPlayer :credit 1)
    (play-from-hand state :resPlayer "Accelerated Diagnostics")

    (let [playarea (get-in @state [:resPlayer :play-area])
          hf (find-card "Hedge Fund" playarea)
          ss (find-card "Shipment from SanSan" playarea)
          bc (find-card "Back Channels" playarea)
          co (get-content state :remote1 0)]
      (is (= 3 (count playarea)) "3 cards in play area")
      (prompt-select :resPlayer ss)
      (prompt-choice :resPlayer "2")
      (prompt-select :resPlayer co)
      (is (= 2 (:advance-counter (refresh co))) "Cerebral Overwriter gained 2 advancements")
      (prompt-select :resPlayer hf)
      (is (= 9 (:credit (get-corp))) "Corp gained credits from Hedge Fund")
      (prompt-select :resPlayer bc)
      (prompt-select :resPlayer (refresh co))
      (is (= 15 (:credit (get-corp))) "Corp gained 6 credits for Back Channels"))))

(deftest accelerated-diagnostics-with-current
  ;; Accelerated Diagnostics - Interaction with Current
  (do-game
    (new-game (default-corp [(qty "Accelerated Diagnostics" 1) (qty "Cerebral Overwriter" 1)
                             (qty "Enhanced Login Protocol" 1) (qty "Shipment from SanSan" 1)
                             (qty "Hedge Fund" 1)])
              (default-runner))
    (starting-hand state :resPlayer ["Accelerated Diagnostics" "Cerebral Overwriter"])
    (play-from-hand state :resPlayer "Cerebral Overwriter" "New remote")
    (core/gain state :resPlayer :credit 3)
    (play-from-hand state :resPlayer "Accelerated Diagnostics")

    (let [playarea (get-in @state [:resPlayer :play-area])
          hf (find-card "Hedge Fund" playarea)
          ss (find-card "Shipment from SanSan" playarea)
          elp (find-card "Enhanced Login Protocol" playarea)
          co (get-content state :remote1 0)]
      (is (= 3 (count playarea)) "3 cards in play area")
      (prompt-select :resPlayer elp)
      (is (= "Enhanced Login Protocol" (:title (first (get-in @state [:resPlayer :current]))))
        "Enhanced Login Protocol active in Current area")
      (prompt-select :resPlayer ss)
      (prompt-choice :resPlayer "2")
      (prompt-select :resPlayer co)
      (is (= 2 (:advance-counter (refresh co))) "Cerebral Overwriter gained 2 advancements")
      (prompt-select :resPlayer hf)
      (is (= 9 (:credit (get-corp))) "Corp gained credits from Hedge Fund"))))

(deftest an-offer-you-cant-refuse
  ;; An Offer You Can't Refuse - exact card added to score area, not the last discarded one
  (do-game
    (new-game (default-corp [(qty "Celebrity Gift" 1) (qty "An Offer You Can't Refuse" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "An Offer You Can't Refuse")
    (prompt-choice :resPlayer "R&D")
    (core/move state :resPlayer (find-card "Celebrity Gift" (:hand (get-corp))) :discard)
    (is (= 2 (count (:discard (get-corp)))))
    (prompt-choice :hazPlayer "No")
    (is (= 1 (:agenda-point (get-corp))) "An Offer the Runner refused")
    (is (= 1 (count (:scored (get-corp)))))
    (is (find-card "An Offer You Can't Refuse" (:scored (get-corp))))
    (is (= 1 (count (:discard (get-corp)))))
    (is (find-card "Celebrity Gift" (:discard (get-corp))))))

(deftest big-brother
  ;; Big Brother - Give the Runner 2 tags if already tagged
  (do-game
    (new-game (default-corp [(qty "Big Brother" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Big Brother")
    (is (= 1 (count (:hand (get-corp)))) "Card not played because Runner has no tags")
    (core/gain state :hazPlayer :tag 1)
    (play-from-hand state :resPlayer "Big Brother")
    (is (= 3 (:tag (get-runner))) "Runner gained 2 tags")))

(deftest biotic-labor
  ;; Biotic Labor - Gain 2 clicks
  (do-game
    (new-game (default-corp [(qty "Biotic Labor" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Biotic Labor")
    (is (= 1 (:credit (get-corp))))
    (is (= 4 (:click (get-corp))) "Spent 1 click to gain 2 additional clicks")))

(deftest blue-level-clearance
  ;; Blue Level Clearance - Gain 5 credits and draw 2 cards
  (do-game
    (new-game (default-corp [(qty "Blue Level Clearance" 3)
                             (qty "Hedge Fund" 3)
                             (qty "Sweeps Week" 2)])
              (default-runner))
    (play-from-hand state :resPlayer "Blue Level Clearance")
    (is (= 8 (:credit (get-corp))) "Gained 5 credits")
    (is (= 1 (:click (get-corp))))
    (is (= 7 (count (:hand (get-corp)))) "Drew 2 cards")))

(deftest casting-call
  ;; Casting Call - Only do card-init on the Public agendas.  Issue #1128
  (do-game
    (new-game (default-corp [(qty "Casting Call" 2) (qty "Oaktown Renovation" 1)
                             (qty "Improved Tracers" 1) (qty "Hunter" 1)])
              (default-runner))
    (core/gain state :resPlayer :click 1)
    (play-from-hand state :resPlayer "Hunter" "HQ")
    (let [hunter (get-ice state :hq 0)]
      (core/rez state :resPlayer hunter)
      (is (= 4 (:current-strength (refresh hunter))))
      (play-from-hand state :resPlayer "Casting Call")
      (prompt-select :resPlayer (find-card "Improved Tracers" (:hand (get-corp))))
      (prompt-choice :resPlayer "New remote")
      (let [imptrac (get-content state :remote1 0)]
        (is (get-in (refresh imptrac) [:rezzed]) "Improved Tracers is faceup")
        (is (= 4 (:current-strength (refresh hunter))) "Hunter hasn't gained strength")
        (play-from-hand state :resPlayer "Casting Call")
        (prompt-select :resPlayer (find-card "Oaktown Renovation" (:hand (get-corp))))
        (prompt-choice :resPlayer "New remote")
        (let [oak (get-content state :remote2 0)]
          (core/advance state :resPlayer {:card (refresh oak)})
          (is (= 5 (:credit (get-corp))) "Events on Public agenda work; gained 2 credits from advancing")
          (take-credits state :resPlayer)
          (run-empty-server state "Server 2")
          (prompt-select :hazPlayer oak)
          (prompt-choice :hazPlayer "Steal")
          (is (= 2 (:tag (get-runner))) "Runner took 2 tags from accessing agenda with Casting Call hosted on it"))))))

(deftest cerebral-cast-runner-wins
  ;; Cerebral Cast: if the runner succefully ran last turn, psi game to give runner choice of tag or BD
  (do-game
    (new-game (default-corp [(qty "Cerebral Cast" 1)])
              (default-runner))
	    (play-from-hand state :resPlayer "Cerebral Cast")
	    (is (= 3 (:click (get-corp))) "Cerebral Cast precondition not met; card not played")
	    (take-credits state :resPlayer)
	    (run-empty-server state "Archives")
	    (take-credits state :hazPlayer)
	    (play-from-hand state :resPlayer "Cerebral Cast")
        (prompt-choice :resPlayer "0 [Credits]")
        (prompt-choice :hazPlayer "0 [Credits]")
	    (is (= 0 (count (:discard (get-runner)))) "Runner took no damage")
		(is (= 0 (:tag (get-runner))) "Runner took no tags")))

(deftest cerebral-cast-corp-wins
  ;; Cerebral Cast: if the runner succefully ran last turn, psi game to give runner choice of tag or BD
  (do-game
    (new-game (default-corp [(qty "Cerebral Cast" 2)])
              (default-runner))
	    (take-credits state :resPlayer)
	    (run-empty-server state "Archives")
	    (take-credits state :hazPlayer)
	    (play-from-hand state :resPlayer "Cerebral Cast")
        (prompt-choice :resPlayer "0 [Credits]")
        (prompt-choice :hazPlayer "1 [Credits]")
		(prompt-choice :hazPlayer "1 brain damage")
	    (is (= 1 (count (:discard (get-runner)))) "Runner took a brain damage")
		(is (= 0 (:tag (get-runner))) "Runner took no tags from brain damage choice")
	    (play-from-hand state :resPlayer "Cerebral Cast")
        (prompt-choice :resPlayer "0 [Credits]")
        (prompt-choice :hazPlayer "1 [Credits]")
		(prompt-choice :hazPlayer "1 tag")
	    (is (= 1 (count (:discard (get-runner)))) "Runner took no additional damage")
		(is (= 1 (:tag (get-runner))) "Runner took a tag from Cerebral Cast choice")))


(deftest cerebral-static-chaos-theory
  ;; Cerebral Static - vs Chaos Theory
  (do-game
    (new-game (default-corp [(qty "Cerebral Static" 1) (qty "Lag Time" 1)])
              (make-deck "Chaos Theory: Wünderkind" [(qty "Sure Gamble" 3)]))
    (is (= 5 (:memory (get-runner))) "CT starts with 5 memory")
    (play-from-hand state :resPlayer "Cerebral Static")
    (is (= 4 (:memory (get-runner))) "Cerebral Static causes CT to have 4 memory")
    (play-from-hand state :resPlayer "Lag Time")
    (is (= 5 (:memory (get-runner))) "CT 5 memory restored")))

(deftest closed-accounts
  ;; Closed Accounts - Play if Runner is tagged to make Runner lose all credits
  (do-game
    (new-game (default-corp [(qty "Closed Accounts" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Closed Accounts")
    (is (and (= 3 (:click (get-corp)))
             (= 5 (:credit (get-runner))))
        "Closed Accounts precondition not met; card not played")
    (core/gain state :hazPlayer :tag 1)
    (play-from-hand state :resPlayer "Closed Accounts")
    (is (= 0 (:credit (get-runner))) "Runner lost all credits")))

(deftest commercialization-single-advancement
  ;; Commercialization - Single advancement token
  (do-game
    (new-game (default-corp [(qty "Commercialization" 1)
                             (qty "Ice Wall" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Ice Wall" "HQ")
    (core/add-counter state :resPlayer (refresh (get-ice state :hq 0)) :advancement 1)
    (play-from-hand state :resPlayer "Commercialization")
    (prompt-select :resPlayer (refresh (get-ice state :hq 0)))
    (is (= 6 (:credit (get-corp))) "Gained 1 for single advanced ice from Commercialization")))

(deftest commercialization-double-advancement
  ;; Commercialization - Two advancement tokens
  (do-game
    (new-game (default-corp [(qty "Commercialization" 1)
                             (qty "Ice Wall" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Ice Wall" "HQ")
    (core/add-counter state :resPlayer (refresh (get-ice state :hq 0)) :advancement 2)
    (play-from-hand state :resPlayer "Commercialization")
    (prompt-select :resPlayer (refresh (get-ice state :hq 0)))
    (is (= 7 (:credit (get-corp))) "Gained 2 for double advanced ice from Commercialization")))

(deftest consulting-visit
  ;; Consulting Visit - Only show single copies of operations corp can afford as choices. Play chosen operation
  (do-game
    (new-game (default-corp [(qty "Consulting Visit" 1)
                             (qty "Beanstalk Royalties" 2)
                             (qty "Green Level Clearance" 1)
                             (qty "Breaking News" 1)
                             (qty "Hedge Fund" 1)])
              (default-runner))
    (is (= 5 (:credit (get-corp))))
    (starting-hand state :resPlayer ["Consulting Visit"])
    (play-from-hand state :resPlayer "Consulting Visit")

    (let [get-prompt (fn [] (first (#(get-in @state [:resPlayer :prompt]))))
          prompt-names (fn [] (map #(:title %) (:choices (get-prompt))))]

      (is (= (list "Beanstalk Royalties" "Green Level Clearance" nil) (prompt-names)))
      (prompt-choice :resPlayer (find-card "Beanstalk Royalties" (:deck (get-corp))))
      (is (= 6 (:credit (get-corp)))))))

(deftest consulting-visit-mumbad
  ;; Consulting Visit - Works properly when played with Mumbad City Hall
  (do-game
    (new-game (default-corp [(qty "Mumbad City Hall" 1)
                             (qty "Beanstalk Royalties" 1)
                             (qty "Green Level Clearance" 1)
                             (qty "Breaking News" 1)
                             (qty "Hedge Fund" 1)
                             (qty "Consulting Visit" 1)
                             (qty "Mumba Temple" 1)])
              (default-runner))
    (is (= 5 (:credit (get-corp))))
    (starting-hand state :resPlayer ["Mumbad City Hall"])
    (play-from-hand state :resPlayer "Mumbad City Hall" "New remote")

    (let [hall (get-content state :remote1 0)
          get-prompt (fn [] (first (#(get-in @state [:resPlayer :prompt]))))
          prompt-names (fn [] (map #(:title %) (:choices (get-prompt))))]

      (card-ability state :resPlayer hall 0)
      (is (= (list "Consulting Visit" "Mumba Temple" nil) (prompt-names)))

      (prompt-choice :resPlayer (find-card "Consulting Visit" (:deck (get-corp))))
      (is (= 3 (:credit (get-corp))))
      (is (= (list "Beanstalk Royalties" "Green Level Clearance" nil) (prompt-names)))

      (prompt-choice :resPlayer (find-card "Green Level Clearance" (:deck (get-corp))))
      (is (= 5 (:credit (get-corp)))))))

(deftest defective-brainchips
  ;; Defective Brainchips - Do 1 add'l brain damage the first time Runner takes some each turn
  (do-game
    (new-game (default-corp [(qty "Defective Brainchips" 1) (qty "Viktor 1.0" 1)])
              (default-runner [(qty "Sure Gamble" 2) (qty "Shiv" 2)]))
    (play-from-hand state :resPlayer "Defective Brainchips")
    (play-from-hand state :resPlayer "Viktor 1.0" "HQ")
    (take-credits state :resPlayer)
    (run-on state :hq)
    (let [vik (get-ice state :hq 0)]
      (core/rez state :resPlayer vik)
      (card-subroutine state :resPlayer vik 0)
      (is (= 2 (count (:discard (get-runner)))) "2 cards lost to brain damage")
      (is (= 2 (:brain-damage (get-runner))) "Brainchips dealt 1 additional brain dmg")
      (card-subroutine state :resPlayer vik 0)
      (is (= 3 (count (:discard (get-runner)))) "2 cards lost to brain damage")
      (is (= 3 (:brain-damage (get-runner))) "Brainchips didn't do additional brain dmg"))))

(deftest distract-the-masses
  (do-game
    (new-game (default-corp [(qty "Distract the Masses" 2) (qty "Hedge Fund" 3)])
              (default-runner))
    (starting-hand state :resPlayer ["Hedge Fund" "Hedge Fund" "Hedge Fund" "Distract the Masses" "Distract the Masses"])
    (play-from-hand state :resPlayer "Distract the Masses")
    (prompt-select :resPlayer (first (:hand (get-corp))))
    (prompt-select :resPlayer (first (next (:hand (get-corp)))))
    (prompt-select :resPlayer (first (:discard (get-corp))))
    (prompt-choice :resPlayer "Done")
    (is (= 1 (count (:discard (get-corp)))) "1 card still discarded")
    (is (= 1 (count (:deck (get-corp)))) "1 card shuffled into R&D")
    (is (= 1 (count (:rfg (get-corp)))) "Distract the Masses removed from game")
    (is (= 7 (:credit (get-runner))) "Runner gained 2 credits")
    (play-from-hand state :resPlayer "Distract the Masses")
    (prompt-select :resPlayer (first (:hand (get-corp))))
    (prompt-choice :resPlayer "Done")
    (prompt-select :resPlayer (first (:discard (get-corp))))
    (prompt-select :resPlayer (first (next (:discard (get-corp)))))
    (is (= 0 (count (:discard (get-corp)))) "No cards left in archives")
    (is (= 3 (count (:deck (get-corp)))) "2 more cards shuffled into R&D")
    (is (= 2 (count (:rfg (get-corp)))) "Distract the Masses removed from game")
    (is (= 9 (:credit (get-runner))) "Runner gained 2 credits")))

(deftest diversified-portfolio
  (do-game
    (new-game (default-corp [(qty "Diversified Portfolio" 1)
                             (qty "Paper Wall" 1)
                             (qty "PAD Campaign" 3)])
              (default-runner))
    (core/gain state :resPlayer :click 2)
    (play-from-hand state :resPlayer "Paper Wall" "New remote")
    (play-from-hand state :resPlayer "PAD Campaign" "New remote")
    (play-from-hand state :resPlayer "PAD Campaign" "New remote")
    (play-from-hand state :resPlayer "PAD Campaign" "New remote")
    (play-from-hand state :resPlayer "Diversified Portfolio")
    (is (= 7 (:credit (get-corp))) "Ignored remote with ICE but no server contents")))

(deftest economic-warfare
  ;; Economic Warfare - If successful run last turn, make the runner lose 4 credits if able
  (do-game
    (new-game (default-corp [(qty "Economic Warfare" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Economic Warfare")
    (is (= 5 (:credit (get-runner))) "Runner has 5 credits")
    (is (= 3 (count (:hand (get-corp)))) "Corp still has 3 cards")
    (take-credits state :resPlayer)

    (run-on state :archives)
    (run-successful state)
    (take-credits state :hazPlayer)

    (play-from-hand state :resPlayer "Economic Warfare")
    (is (= 4 (:credit (get-runner))) "Runner has 4 credits")
    (play-from-hand state :resPlayer "Economic Warfare")
    (is (= 0 (:credit (get-runner))) "Runner has 0 credits")
    (take-credits state :resPlayer)

    (run-on state :archives)
    (take-credits state :hazPlayer)

    (play-from-hand state :resPlayer "Economic Warfare")
    (is (= 3 (:credit (get-runner))) "Runner has 3 credits")))

(deftest enhanced-login-protocol
  ;; Enhanced Login Protocol - First click run each turn costs an additional click
  (do-game
    (new-game (default-corp [(qty "Enhanced Login Protocol" 1)])
              (default-runner [(qty "Employee Strike" 1)]))
    (play-from-hand state :resPlayer "Enhanced Login Protocol")
    (take-credits state :resPlayer)

    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (run-on state :archives)
    (is (= 2 (:click (get-runner)))
        "Runner spends 1 additional click to make the first run")
    (run-successful state)

    (run-on state :archives)
    (is (= 1 (:click (get-runner)))
        "Runner doesn't spend 1 additional click to make the second run")
    (run-successful state)

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer 3)

    (is (= 1 (:click (get-runner))) "Runner has 1 click")
    (run-on state :archives)
    (is (not (:run @state)) "No run was initiated")
    (is (= 1 (:click (get-runner))) "Runner has 1 click")

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)

    (play-from-hand state :hazPlayer "Employee Strike")

    (is (= 3 (:click (get-runner))) "Runner has 3 clicks")
    (run-on state :archives)
    (is (= 2 (:click (get-runner)))
        "Runner doesn't spend 1 additional click to make a run")))

(deftest enhanced-login-protocol-card-ability
  ;; Enhanced Login Protocol - Card ability runs don't cost additional clicks
  (do-game
    (new-game (default-corp [(qty "Enhanced Login Protocol" 1)])
              (default-runner [(qty "Sneakdoor Beta" 1)]))
    (play-from-hand state :resPlayer "Enhanced Login Protocol")
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Sneakdoor Beta")
    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)

    (is (= 4 (:click (get-runner))) "Runner has 2 clicks")
    (let [sneakdoor (get-in @state [:hazPlayer :rig :program 0])]
      (card-ability state :hazPlayer sneakdoor 0)
      (is (= 3 (:click (get-runner)))
          "Runner doesn't spend 1 additional click to run with a card ability")
      (run-successful state)

      (run-on state :archives)
      (is (= 1 (:click (get-runner)))
          "Runner spends 1 additional click to make a run")
      (run-successful state)

      (take-credits state :hazPlayer)
      (take-credits state :resPlayer)

      (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
      (run-on state :archives)
      (is (= 2 (:click (get-runner)))
          "Runner spends 1 additional click to make a run"))))

(deftest enhanced-login-protocol-new-angeles-sol
  ;; Enhanced Login Protocol trashed and reinstalled on steal doesn't double remove penalty
  (do-game
    (new-game
      (make-deck "New Angeles Sol: Your News" [(qty "Enhanced Login Protocol" 1) (qty "Breaking News" 1)])
      (default-runner))
    (play-from-hand state :resPlayer "Breaking News" "New remote")
    (play-from-hand state :resPlayer "Enhanced Login Protocol")
    (take-credits state :resPlayer)

    (run-on state :remote1)
    (run-successful state)
    (prompt-choice :hazPlayer "Steal")

    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Enhanced Login Protocol"
                                    (:discard (get-corp))))

    (run-on state :archives)
    (is (= 1 (:click (get-runner))) "Runner has 1 click")))

(deftest enhanced-login-protocol-run-events
  ;; Enhanced Login Protocol - Run event don't cost additional clicks
  (do-game
    (new-game (default-corp [(qty "Enhanced Login Protocol" 1)])
              (default-runner [(qty "Out of the Ashes" 1)]))
    (play-from-hand state :resPlayer "Enhanced Login Protocol")
    (take-credits state :resPlayer)

    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (play-from-hand state :hazPlayer "Out of the Ashes")
    (prompt-choice :hazPlayer "Archives")
    (is (= 3 (:click (get-runner)))
        "Runner doesn't spend 1 additional click to run with a run event")
    (run-successful state)

    (run-on state :archives)
    (is (= 1 (:click (get-runner)))
        "Runner spends 1 additional click to make a run")
    (run-successful state)

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)
    (prompt-choice :hazPlayer "No") ; Out of the Ashes prompt

    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (run-on state :archives)
    (is (= 2 (:click (get-runner)))
        "Runner spends 1 additional click to make a run")))

(deftest enhanced-login-protocol-runner-turn-first-run
  ;; Enhanced Login Protocol - Works when played on the runner's turn
  (do-game
    (new-game (make-deck "New Angeles Sol: Your News"
                         [(qty "Enhanced Login Protocol" 1)
                          (qty "Breaking News" 1)])
              (default-runner [(qty "Hades Shard" 1)]))
    (trash-from-hand state :resPlayer "Breaking News")
    (take-credits state :resPlayer)

    (core/gain state :hazPlayer :credit 2)
    (play-from-hand state :hazPlayer "Hades Shard")
    (card-ability state :hazPlayer (get-in @state [:hazPlayer :rig :resource 0]) 0)
    (prompt-choice :hazPlayer "Steal")
    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Enhanced Login Protocol"
                                    (:hand (get-corp))))
    (is (find-card "Enhanced Login Protocol" (:current (get-corp)))
        "Enhanced Login Protocol is in play")

    (is (= 3 (:click (get-runner))) "Runner has 3 clicks")
    (run-on state :archives)
    (is (= 1 (:click (get-runner)))
        "Runner spends 1 additional click to make a run")))

(deftest enhanced-login-protocol-runner-turn-second-run
  ;; Enhanced Login Protocol - Doesn't fire if already run when played on the runner's turn
  (do-game
    (new-game (make-deck "New Angeles Sol: Your News"
                         [(qty "Enhanced Login Protocol" 1)
                          (qty "Breaking News" 1)])
              (default-runner [(qty "Hades Shard" 1)]))
    (trash-from-hand state :resPlayer "Breaking News")
    (take-credits state :resPlayer)

    (run-on state :hq)
    (run-successful state)
    (prompt-choice :hazPlayer "OK")

    (core/gain state :hazPlayer :credit 2)
    (play-from-hand state :hazPlayer "Hades Shard")
    (card-ability state :hazPlayer (get-in @state [:hazPlayer :rig :resource 0]) 0)
    (prompt-choice :hazPlayer "Steal")
    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Enhanced Login Protocol"
                                    (:hand (get-corp))))
    (is (find-card "Enhanced Login Protocol" (:current (get-corp)))
        "Enhanced Login Protocol is in play")

    (is (= 2 (:click (get-runner))) "Runner has 2 clicks")
    (run-on state :archives)
    (is (= 1 (:click (get-runner)))
        "Runner doesn't spend 1 additional click to make a run")))

(deftest exchange-of-information
  ;; Exchange of Information - Swapping agendas works correctly
  (do-game
    (new-game (default-corp [(qty "Exchange of Information" 1)
                             (qty "Market Research" 1)
                             (qty "Breaking News" 1)
                             (qty "Project Beale" 1)
                             (qty "Explode-a-palooza" 1)])
              (default-runner))

      (score-agenda state :resPlayer (find-card "Market Research" (:hand (get-corp))))
      (score-agenda state :resPlayer (find-card "Breaking News" (:hand (get-corp))))
      (is (= 2 (:tag (get-runner))) "Runner gained 2 tags")
      (take-credits state :resPlayer)
      (is (= 0 (:tag (get-runner))) "Runner lost 2 tags")

      (core/steal state :hazPlayer (find-card "Project Beale" (:hand (get-corp))))
      (core/steal state :hazPlayer (find-card "Explode-a-palooza" (:hand (get-corp))))
      (take-credits state :hazPlayer)

      (is (= 4 (:agenda-point (get-runner))))
      (is (= 3 (:agenda-point (get-corp))))

      (core/gain state :hazPlayer :tag 1)
      (play-from-hand state :resPlayer "Exchange of Information")

      (prompt-select :resPlayer (find-card "Project Beale" (:scored (get-runner))))
      (prompt-select :resPlayer (find-card "Breaking News" (:scored (get-corp))))

      (is (= 3 (:agenda-point (get-runner))))
      (is (= 4 (:agenda-point (get-corp))))))

(deftest exchange-of-information-breaking-news
  ;; Exchange of Information - Swapping a just scored Breaking News keeps the tags
  (do-game
    (new-game (default-corp [(qty "Exchange of Information" 1)
                             (qty "Market Research" 1)
                             (qty "Breaking News" 1)
                             (qty "Project Beale" 1)
                             (qty "Explode-a-palooza" 1)])
              (default-runner))

      (take-credits state :resPlayer)

      (core/steal state :hazPlayer (find-card "Project Beale" (:hand (get-corp))))
      (core/steal state :hazPlayer (find-card "Explode-a-palooza" (:hand (get-corp))))
      (take-credits state :hazPlayer)

      (score-agenda state :resPlayer (find-card "Breaking News" (:hand (get-corp))))
      (is (= 2 (:tag (get-runner))) "Runner gained 2 tags")
      (play-from-hand state :resPlayer "Exchange of Information")

      (prompt-select :resPlayer (find-card "Project Beale" (:scored (get-runner))))
      (prompt-select :resPlayer (find-card "Breaking News" (:scored (get-corp))))
      (is (= 2 (:tag (get-runner))) "Still has tags after swap and before end of turn")

      (take-credits state :resPlayer)
      (is (= 3 (:agenda-point (get-runner))))
      (is (= 2 (:agenda-point (get-corp))))
      (is (= 2 (:tag (get-runner))) "Runner does not lose tags at end of turn")))

(deftest exchange-of-information-fifteen-minutes
  ;; Exchange of Information - Swapping a 15 Minutes still keeps the ability. #1783
  (do-game
    (new-game (default-corp [(qty "Exchange of Information" 2) (qty "15 Minutes" 1)
                             (qty "Project Beale" 1)])
              (default-runner))
    (score-agenda state :resPlayer (find-card "15 Minutes" (:hand (get-corp))))
    (take-credits state :resPlayer)
    (core/gain state :hazPlayer :tag 1)
    (core/steal state :hazPlayer (find-card "Project Beale" (:hand (get-corp))))
    (take-credits state :hazPlayer)
    (is (= 1 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    (play-from-hand state :resPlayer "Exchange of Information")
    (prompt-select :resPlayer (find-card "Project Beale" (:scored (get-runner))))
    (prompt-select :resPlayer (find-card "15 Minutes" (:scored (get-corp))))
    (is (= 2 (:agenda-point (get-corp))))
    (is (= 1 (:agenda-point (get-runner))))
    (is (= 0 (count (:deck (get-corp)))))
    ;; shuffle back into R&D from runner's scored area
    (let [fifm (get-in @state [:hazPlayer :scored 0])]
      (card-ability state :resPlayer fifm 0))
    (is (= 2 (:agenda-point (get-corp))))
    (is (= 0 (:agenda-point (get-runner))))
    (is (= "15 Minutes" (:title (first (:deck (get-corp))))))
    (take-credits state :resPlayer)
    (core/steal state :hazPlayer (find-card "15 Minutes" (:deck (get-corp))))
    (take-credits state :hazPlayer)
    (is (= 2 (:agenda-point (get-corp))))
    (is (= 1 (:agenda-point (get-runner))))
    (play-from-hand state :resPlayer "Exchange of Information")
    (prompt-select :resPlayer (find-card "15 Minutes" (:scored (get-runner))))
    (prompt-select :resPlayer (find-card "Project Beale" (:scored (get-corp))))
    (is (= 1 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    ;; shuffle back into R&D from corp's scored area
    (let [fifm (get-in @state [:resPlayer :scored 0])]
      (card-ability state :resPlayer fifm 0))
    (is (= "15 Minutes" (:title (first (:deck (get-corp))))))))

(deftest exchange-of-information-mandatory-upgrades
  ;; Exchange of Information - Swapping a Mandatory Upgrades gives the Corp an additional click per turn. #1687
  (do-game
    (new-game (default-corp [(qty "Exchange of Information" 2) (qty "Mandatory Upgrades" 1)
                             (qty "Global Food Initiative" 1)])
              (default-runner))
    (score-agenda state :resPlayer (find-card "Global Food Initiative" (:hand (get-corp))))
    (take-credits state :resPlayer)
    (core/gain state :hazPlayer :tag 1)
    (core/steal state :hazPlayer (find-card "Mandatory Upgrades" (:hand (get-corp))))
    (take-credits state :hazPlayer)
    (is (= 3 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    (is (= 3 (:click (get-corp))))
    (is (= 3 (:click-per-turn (get-corp))))
    (play-from-hand state :resPlayer "Exchange of Information")
    (prompt-select :resPlayer (find-card "Mandatory Upgrades" (:scored (get-runner))))
    (prompt-select :resPlayer (find-card "Global Food Initiative" (:scored (get-corp))))
    (is (= 2 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    (is (= 3 (:click (get-corp))))
    (is (= 4 (:click-per-turn (get-corp))))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (is (= 4 (:click (get-corp))))
    (is (= 4 (:click-per-turn (get-corp))))
    (play-from-hand state :resPlayer "Exchange of Information")
    (prompt-select :resPlayer (find-card "Global Food Initiative" (:scored (get-runner))))
    (prompt-select :resPlayer (find-card "Mandatory Upgrades" (:scored (get-corp))))
    (is (= 3 (:agenda-point (get-corp))))
    (is (= 2 (:agenda-point (get-runner))))
    (is (= 2 (:click (get-corp))))
    (is (= 3 (:click-per-turn (get-corp))))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (is (= 3 (:click (get-corp))))
    (is (= 3 (:click-per-turn (get-corp))))))

(deftest election-day
  (do-game
    (new-game (default-corp [(qty "Election Day" 7)])
                (default-runner))
      (is (= 6 (count (:hand (get-corp)))) "Corp starts with 5 + 1 cards")
      (core/move state :resPlayer (find-card "Election Day" (:hand (get-corp))) :deck)
      (core/move state :resPlayer (find-card "Election Day" (:hand (get-corp))) :deck)
      (core/move state :resPlayer (find-card "Election Day" (:hand (get-corp))) :deck)
      (core/move state :resPlayer (find-card "Election Day" (:hand (get-corp))) :deck)
      (core/move state :resPlayer (find-card "Election Day" (:hand (get-corp))) :deck)
      (play-from-hand state :resPlayer "Election Day")
      (is (= 1 (count (:hand (get-corp)))) "Could not play Election Day")
      (take-credits state :resPlayer)
      (take-credits state :hazPlayer)
      (is (= 2 (count (:hand (get-corp)))) "Corp has now 1 + 1 cards before Election Day")
      (play-from-hand state :resPlayer "Election Day")
      (is (= 5 (count (:hand (get-corp)))) "Corp has now 5 cards due to Election Day")))

(deftest hedge-fund
  (do-game
    (new-game (default-corp) (default-runner))
    (is (= 5 (:credit (get-corp))))
    (play-from-hand state :resPlayer "Hedge Fund")
    (is (= 9 (:credit (get-corp))))))

(deftest housekeeping
  ;; Housekeeping - Runner must trash a card from Grip on first install of a turn
  (do-game
    (new-game (default-corp [(qty "Housekeeping" 1)])
              (default-runner [(qty "Cache" 2) (qty "Fall Guy" 1) (qty "Mr. Li" 1)]))
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Fall Guy")
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Housekeeping")
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Cache")
    (prompt-select :hazPlayer (find-card "Mr. Li" (:hand (get-runner))))
    (is (empty? (:prompt (get-runner))) "Fall Guy prevention didn't trigger")
    (is (= 1 (count (:discard (get-runner)))) "Card trashed")
    (play-from-hand state :hazPlayer "Cache")
    (is (empty? (:prompt (get-runner))) "Housekeeping didn't trigger on 2nd install")))

(deftest invasion-of-privacy
  ;; Invasion of Privacy - Full test
  (do-game
    (new-game (default-corp [(qty "Invasion of Privacy" 3)])
              (default-runner [(qty "Sure Gamble" 2) (qty "Fall Guy" 1) (qty "Cache" 2)]))
    (core/gain state :resPlayer :click 3 :credit 6)
    ;; trash 2 cards
    (play-from-hand state :resPlayer "Invasion of Privacy")
    (prompt-choice :resPlayer 0) ; default trace
    (prompt-choice :hazPlayer 0) ; Runner won't match
    (is (= 5 (count (:hand (get-runner)))))
    (let [get-prompt (fn [] (first (#(get-in @state [:resPlayer :prompt]))))
          prompt-names (fn [] (map #(:title %) (:choices (get-prompt))))]
      (is (= (list "Fall Guy" "Sure Gamble" nil) (prompt-names)))
      (prompt-choice :resPlayer (find-card "Sure Gamble" (:hand (get-runner))))
      (prompt-choice :resPlayer (find-card "Sure Gamble" (:hand (get-runner)))))
    (is (= 3 (count (:hand (get-runner)))))
    ;; able to trash 2 cards but only 1 available target in Runner's hand
    (play-from-hand state :resPlayer "Invasion of Privacy")
    (prompt-choice :resPlayer 0) ; default trace
    (prompt-choice :hazPlayer 0) ; Runner won't match
    (is (= 3 (count (:hand (get-runner)))))
    (let [get-prompt (fn [] (first (#(get-in @state [:resPlayer :prompt]))))
          prompt-names (fn [] (map #(:title %) (:choices (get-prompt))))]
      (is (= (list "Fall Guy" nil) (prompt-names)))
      (prompt-choice :resPlayer (find-card "Fall Guy" (:hand (get-runner))))
      (is (empty? (get-in @state [:resPlayer :prompt])) "No prompt for second card"))
    (is (= 2 (count (:hand (get-runner)))))
    ;; failed trace - take the bad publicity
    (play-from-hand state :resPlayer "Invasion of Privacy")
    (prompt-choice :resPlayer 0) ; default trace
    (prompt-choice :hazPlayer 2) ; Runner matches
    (is (= 1 (:bad-publicity (get-corp))))))

(deftest ipo-terminal
  ;; IPO - credits with Terminal operations
  (do-game
    (new-game
      (default-corp [(qty "IPO" 1)])
      (default-runner))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "IPO")
	(is (= 13 (:credit (get-corp))))
	(is (= 0 (:click (get-corp))) "Terminal ends turns")))

(deftest lag-time
  (do-game
    (new-game (default-corp [(qty "Lag Time" 1) (qty "Vanilla" 1) (qty "Lotus Field" 1)])
              (default-runner))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Vanilla" "HQ")
    (play-from-hand state :resPlayer "Lotus Field" "R&D")
    (play-from-hand state :resPlayer "Lag Time")
    (core/rez state :resPlayer (get-ice state :hq 0))
    (core/rez state :resPlayer (get-ice state :rd 0))
    (is (= 1 (:current-strength (get-ice state :hq 0))) "Vanilla at 1 strength")
	(is (= 5 (:current-strength (get-ice state :rd 0))) "Lotus Field at 5 strength")))

(deftest lateral-growth
  (do-game
    (new-game (default-corp [(qty "Lateral Growth" 1) (qty "Breaking News" 1)])
              (default-runner))
    (is (= 5 (:credit (get-corp))))
    (play-from-hand state :resPlayer "Lateral Growth")
    (prompt-select :resPlayer (find-card "Breaking News" (:hand (get-corp))))
    (prompt-choice :resPlayer "New remote")
    (is (= "Breaking News" (:title (get-content state :remote1 0)))
      "Breaking News installed by Lateral Growth")
    (is (= 7 (:credit (get-corp))))))

(deftest mass-commercialization
  ;; Mass Commercialization
  (do-game
    (new-game (default-corp [(qty "Mass Commercialization" 1)
                             (qty "Ice Wall" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Ice Wall" "HQ")
    (play-from-hand state :resPlayer "Ice Wall" "R&D")
    (play-from-hand state :resPlayer "Ice Wall" "Archives")
    (take-credits state :hazPlayer)
    (core/advance state :resPlayer {:card (refresh (get-ice state :hq 0))})
    (core/advance state :resPlayer {:card (refresh (get-ice state :archives 0))})
    (core/advance state :resPlayer {:card (refresh (get-ice state :rd 0))})
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Mass Commercialization")
    (is (= 8 (:credit (get-corp))) "Gained 6 for 3 advanced ice from Mass Commercialization")))

(deftest manhunt-every-run
  ;; Manhunt - only fires once per turn. Unreported issue.
  (do-game
    (new-game (default-corp [(qty "Manhunt" 1) (qty "Hedge Fund" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Manhunt")
    (take-credits state :resPlayer)
    (run-empty-server state "HQ")
    (is (:prompt (get-corp)) "Manhunt trace initiated")
    (prompt-choice :resPlayer 0)
    (prompt-choice :hazPlayer 0)
    (is (= 1 (:tag (get-runner))) "Runner took 1 tag")
    (prompt-choice :hazPlayer "OK")
    (is (not (:run @state)) "Run ended")
    (run-empty-server state "HQ")
    (is (empty? (:prompt (get-corp))) "No Manhunt trace on second run")
    (prompt-choice :hazPlayer "OK")
    (is (not (:run @state)) "Run ended")))

(deftest midseason-replacements
  ;; Midseason Replacements - Trace to give Runner tags after they steal an agenda
  (do-game
    (new-game (default-corp [(qty "Midseason Replacements" 1) (qty "Breaking News" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Midseason Replacements")
    (is (= 3 (:click (get-corp))) "Midseason precondition not met; Corp not charged a click")
    (play-from-hand state :resPlayer "Breaking News" "New remote")
    (take-credits state :resPlayer)
    (is (= 7 (:credit (get-corp))))
    (let [bn (get-content state :remote1 0)]
      (run-empty-server state "Server 1")
      (prompt-choice :hazPlayer "Steal")
      (is (= 1 (:agenda-point (get-runner))) "Stole Breaking News")
      (take-credits state :hazPlayer)
      (play-from-hand state :resPlayer "Midseason Replacements")
      (prompt-choice :resPlayer 0) ; default trace
      (prompt-choice :hazPlayer 0) ; Runner won't match
      (is (= 6 (:tag (get-runner))) "Runner took 6 tags"))))

(deftest mushin-no-shin
  ;; Mushin No Shin - Add 3 advancements to a card; prevent rez/score of that card the rest of the turn
  (do-game
    (new-game (default-corp [(qty "Mushin No Shin" 2) (qty "Ronin" 1) (qty "Profiteering" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Mushin No Shin")
    (prompt-select :resPlayer (find-card "Ronin" (:hand (get-corp))))
    (let [ronin (get-content state :remote1 0)]
      (is (= 3 (:advance-counter (refresh ronin))) "3 advancements placed on Ronin")
      (core/rez state :resPlayer (refresh ronin))
      (is (not (get-in (refresh ronin) [:rezzed])) "Ronin did not rez")
      (take-credits state :resPlayer)
      (take-credits state :hazPlayer)
      (core/rez state :resPlayer (refresh ronin))
      (is (get-in (refresh ronin) [:rezzed]) "Ronin now rezzed")
      (play-from-hand state :resPlayer "Mushin No Shin")
      (prompt-select :resPlayer (find-card "Profiteering" (:hand (get-corp))))
      (let [prof (get-content state :remote2 0)]
        (core/score state :resPlayer (refresh prof))
        (is (empty? (:scored (get-corp))) "Profiteering not scored")
        (is (= 0 (:agenda-point (get-corp))))
        (take-credits state :resPlayer)
        (take-credits state :hazPlayer)
        (core/score state :resPlayer (refresh prof))
        (prompt-choice :resPlayer "0")
        (is (= 1 (:agenda-point (get-corp))) "Profiteering was able to be scored")))))

(deftest neural-emp
  ;; Neural EMP - Play if Runner made a run the previous turn to do 1 net damage
  (do-game
    (new-game (default-corp [(qty "Neural EMP" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Neural EMP")
    (is (= 3 (:click (get-corp))) "Neural precondition not met; card not played")
    (take-credits state :resPlayer)
    (run-empty-server state "Archives")
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Neural EMP")
    (is (= 1 (count (:discard (get-runner)))) "Runner took 1 net damage")))

(deftest oversight-ai
  ;; Oversight AI - Rez a piece of ICE ignoring all costs
  (do-game
    (new-game (default-corp [(qty "Oversight AI" 1) (qty "Archer" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Archer" "R&D")
    (let [archer (get-ice state :rd 0)]
      (play-from-hand state :resPlayer "Oversight AI")
      (prompt-select :resPlayer archer)
      (is (get-in (refresh archer) [:rezzed]))
      (is (= 4 (:credit (get-corp))) "Archer rezzed at no credit cost")
      (is (= "Oversight AI" (:title (first (:hosted (refresh archer)))))
          "Archer hosting OAI as a condition"))))

(deftest patch
  ;; Patch - +2 current strength
  (do-game
    (new-game (default-corp [(qty "Patch" 1) (qty "Vanilla" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Vanilla" "HQ")
    (core/rez state :resPlayer (get-ice state :hq 0))
    (play-from-hand state :resPlayer "Patch")
    (prompt-select :resPlayer (get-ice state :hq 0))
    (is (= 2 (:current-strength (get-ice state :hq 0))) "Vanilla at 2 strength")))

(deftest paywall-implementation
  ;; Paywall Implementation - Gain 1 credit for every successful run
  (do-game
    (new-game (default-corp [(qty "Paywall Implementation" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Paywall Implementation")
    (is (= "Paywall Implementation" (:title (first (get-in @state [:resPlayer :current]))))
        "Paywall active in Current area")
    (take-credits state :resPlayer)
    (is (= 7 (:credit (get-corp))))
    (run-empty-server state "Archives")
    (is (= 8 (:credit (get-corp))) "Gained 1 credit from successful run")
    (run-empty-server state "Archives")
    (is (= 9 (:credit (get-corp))) "Gained 1 credit from successful run")))

(deftest peak-efficiency
  ;; Peak Efficiency - Gain 1 credit for each rezzed ICE
  (do-game
    (new-game (default-corp [(qty "Peak Efficiency" 1) (qty "Paper Wall" 3) (qty "Wraparound" 1)])
              (default-runner))
    (core/gain state :resPlayer :click 3)
    (play-from-hand state :resPlayer "Paper Wall" "HQ")
    (play-from-hand state :resPlayer "Paper Wall" "R&D")
    (play-from-hand state :resPlayer "Paper Wall" "New remote")
    (play-from-hand state :resPlayer "Wraparound" "New remote")
    (core/rez state :resPlayer (get-ice state :hq 0))
    (core/rez state :resPlayer (get-ice state :rd 0))
    (core/rez state :resPlayer (get-ice state :remote1 0))
    (play-from-hand state :resPlayer "Peak Efficiency")
    (is (= 7 (:credit (get-corp))) "Gained 3 credits for 3 rezzed ICE; unrezzed ICE ignored")))

(deftest power-shutdown
  ;; Power Shutdown - Trash cards from R&D to force Runner to trash a program or hardware
  (do-game
    (new-game (default-corp [(qty "Power Shutdown" 3) (qty "Hive" 3)])
              (default-runner [(qty "Grimoire" 1) (qty "Cache" 1)]))
    (play-from-hand state :resPlayer "Power Shutdown")
    (is (empty? (:discard (get-corp))) "Not played, no run last turn")
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Cache")
    (play-from-hand state :hazPlayer "Grimoire")
    (run-empty-server state :archives)
    (take-credits state :hazPlayer)
    (core/move state :resPlayer (find-card "Hive" (:hand (get-corp))) :deck)
    (core/move state :resPlayer (find-card "Hive" (:hand (get-corp))) :deck)
    (core/move state :resPlayer (find-card "Hive" (:hand (get-corp))) :deck)
    (play-from-hand state :resPlayer "Power Shutdown")
    (prompt-choice :resPlayer 2)
    (is (= 3 (count (:discard (get-corp)))) "2 cards trashed from R&D")
    (is (= 1 (count (:deck (get-corp)))) "1 card remaining in R&D")
    (prompt-select :hazPlayer (get-in @state [:hazPlayer :rig :hardware 0])) ; try targeting Grimoire
    (is (empty? (:discard (get-runner))) "Grimoire too expensive to be targeted")
    (prompt-select :hazPlayer (get-in @state [:hazPlayer :rig :program 0]))
    (is (= 1 (count (:discard (get-runner)))) "Cache trashed")))

(deftest precognition
  ;; Precognition - Full test
  (do-game
    (new-game (default-corp [(qty "Precognition" 1) (qty "Caprice Nisei" 1) (qty "Adonis Campaign" 1)
                             (qty "Quandary" 1) (qty "Jackson Howard" 1) (qty "Global Food Initiative" 1)])
              (default-runner))
    (starting-hand state :resPlayer ["Precognition"])
    (play-from-hand state :resPlayer "Precognition")
    (prompt-choice :resPlayer (find-card "Caprice Nisei" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Adonis Campaign" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Quandary" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Jackson Howard" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Global Food Initiative" (:deck (get-corp))))
    ;; try starting over
    (prompt-choice :resPlayer "Start over")
    (prompt-choice :resPlayer (find-card "Global Food Initiative" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Jackson Howard" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Quandary" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Adonis Campaign" (:deck (get-corp))))
    (prompt-choice :resPlayer (find-card "Caprice Nisei" (:deck (get-corp)))) ;this is the top card of R&D
    (prompt-choice :resPlayer "Done")
    (is (= "Caprice Nisei" (:title (first (:deck (get-corp))))))
    (is (= "Adonis Campaign" (:title (second (:deck (get-corp))))))
    (is (= "Quandary" (:title (second (rest (:deck (get-corp)))))))
    (is (= "Jackson Howard" (:title (second (rest (rest (:deck (get-corp))))))))
    (is (= "Global Food Initiative" (:title (second (rest (rest (rest (:deck (get-corp)))))))))))

(deftest preemptive-action
  ;; Preemptive Action - Shuffles cards into R&D and removes itself from game
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 3)
                             (qty "Preemptive Action" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Preemptive Action")
    (prompt-select :resPlayer (first (:discard (get-corp))))
    (prompt-select :resPlayer (second (:discard (get-corp))))
    (prompt-select :resPlayer (last (:discard (get-corp))))
    (is (= 0 (count (:discard (get-corp)))))
    (is (= 1 (count (:rfg (get-corp)))))))

(deftest psychographics
  ;; Psychographics - Place advancements up to the number of Runner tags on a card
  (do-game
    (new-game (default-corp [(qty "Psychographics" 1) (qty "Project Junebug" 1)])
              (default-runner))
    (core/gain state :hazPlayer :tag 4)
    (play-from-hand state :resPlayer "Project Junebug" "New remote")
    (let [pj (get-content state :remote1 0)]
      (play-from-hand state :resPlayer "Psychographics")
      (prompt-choice :resPlayer 4)
      (prompt-select :resPlayer pj)
      (is (= 1 (:credit (get-corp))) "Spent 4 credits")
      (is (= 4 (:advance-counter (refresh pj))) "Junebug has 4 advancements"))))

(deftest psychokinesis
  ;; Pyschokinesis - Terminal Event (end the turn); Look at R&D, install an Asset, Agenda, or Upgrade in a Remote Server
  (do-game
    (new-game (default-corp [(qty "Psychokinesis" 3) (qty "Caprice Nisei" 1) (qty "Adonis Campaign" 1)
                              (qty "Global Food Initiative" 1)])
              (default-runner))
    (starting-hand state :resPlayer ["Psychokinesis","Psychokinesis","Psychokinesis"])
    ;; Test installing an Upgrade
    (play-from-hand state :resPlayer "Psychokinesis")
    (prompt-choice :resPlayer (find-card "Caprice Nisei" (:deck (get-corp))))
    (prompt-choice :resPlayer "New remote")
    (is (= "Caprice Nisei" (:title (get-content state :remote1 0)))
      "Caprice Nisei installed by Psychokinesis")
    ;; Test installing an Asset
    (core/gain state :resPlayer :click 1)
    (play-from-hand state :resPlayer "Psychokinesis")
    (prompt-choice :resPlayer (find-card "Adonis Campaign" (:deck (get-corp))))
    (prompt-choice :resPlayer "New remote")
    (is (= "Adonis Campaign" (:title (get-content state :remote2 0)))
      "Adonis Campaign installed by Psychokinesis")
    ;; Test installing an Agenda
    (core/gain state :resPlayer :click 1)
    (play-from-hand state :resPlayer "Psychokinesis")
    (prompt-choice :resPlayer (find-card "Global Food Initiative" (:deck (get-corp))))
    (prompt-choice :resPlayer "New remote")
    (is (= "Global Food Initiative" (:title (get-content state :remote3 0)))
      "Global Food Initiative installed by Psychokinesis")
    ;; Test selecting "None"
    (core/gain state :resPlayer :click 1)
    (core/move state :resPlayer (find-card "Psychokinesis" (:discard (get-corp))) :hand)
    (play-from-hand state :resPlayer "Psychokinesis")
    (prompt-choice :resPlayer "None")
    (is (= nil (:title (get-content state :remote4 0)))
      "Nothing is installed by Psychokinesis")))

(deftest punitive-counterstrike
  ;; Punitive Counterstrike - deal meat damage equal to printed agenda points
  (do-game
    (new-game (default-corp [(qty "Global Food Initiative" 1) (qty "Punitive Counterstrike" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Global Food Initiative" "New remote")
    (take-credits state :resPlayer)
    (run-empty-server state :remote1)
    (prompt-choice :hazPlayer "Steal")
    (is (= 2 (:agenda-point (get-runner))) "Runner scored 2 points")
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Punitive Counterstrike")
    (prompt-choice :resPlayer 0)
    (prompt-choice :hazPlayer 0)
    (is (empty? (:hand (get-runner))) "Runner took 3 meat damage")))

(deftest reuse
  ;; Reuse - Gain 2 credits for each card trashed from HQ
  (do-game
    (new-game (default-corp [(qty "Reuse" 2) (qty "Hive" 1) (qty "IQ" 1)
                             (qty "Ice Wall" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Reuse")
    (prompt-select :resPlayer (find-card "Ice Wall" (:hand (get-corp))))
    (prompt-select :resPlayer (find-card "Hive" (:hand (get-corp))))
    (prompt-select :resPlayer (find-card "IQ" (:hand (get-corp))))
    (prompt-choice :resPlayer "Done")
    (is (= 4 (count (:discard (get-corp)))) "3 cards trashed plus operation played")
    (is (= 11 (:credit (get-corp))) "Gained 6 credits")
    (is (= 1 (:click (get-corp))) "Spent 2 clicks")))

(deftest salems-hospitality
  ;; Salem's Hospitality - Full test
  (do-game
    (new-game (default-corp [(qty "Salem's Hospitality" 3)])
              (default-runner [(qty "I've Had Worse" 3) (qty "Faust" 1)
                               (qty "Levy AR Lab Access" 1)]))
    (play-from-hand state :resPlayer "Salem's Hospitality")
    (is (= 5 (count (:hand (get-runner)))))
    (prompt-choice :resPlayer "I've Had Worse")
    (is (= 2 (count (:hand (get-runner)))))
    (play-from-hand state :resPlayer "Salem's Hospitality")
    (prompt-choice :resPlayer "Plascrete Carapace")
    (is (= 2 (count (:hand (get-runner)))))))

(deftest scorched-earth
  ;; Scorched Earth - burn 'em
  (do-game
    (new-game (default-corp [(qty "Scorched Earth" 1)])
              (default-runner [(qty "Sure Gamble" 3) (qty "Lucky Find" 3)]))
    (core/gain state :hazPlayer :tag 1)
    (play-from-hand state :resPlayer "Scorched Earth")
    (is (= 1 (count (:hand (get-runner)))) "Runner has 1 card in hand")))

(deftest scorched-earth-no-tag
  ;; Scorched Earth - not tagged
  (do-game
    (new-game (default-corp [(qty "Scorched Earth" 1)])
              (default-runner [(qty "Sure Gamble" 3) (qty "Lucky Find" 3)]))
    (play-from-hand state :resPlayer "Scorched Earth")
    (is (= 3 (:click (get-corp))) "Corp not charged a click")
    (is (= 5 (count (:hand (get-runner)))) "Runner did not take damage")))

(deftest scorched-earth-flatline
  ;; Scorched Earth - murderize 'em
  (do-game
    (new-game (default-corp [(qty "Scorched Earth" 10)])
              (default-runner))
    (core/gain state :hazPlayer :tag 1)
    (play-from-hand state :resPlayer "Scorched Earth")
    (is (= 0 (count (:hand (get-runner)))) "Runner has 0 cards in hand")
    (is (= :resPlayer (:winner @state)) "Corp wins")
    (is (= "Flatline" (:reason @state)) "Win condition reports flatline")))

(deftest subcontract-scorched
  ;; Subcontract - Don't allow second operation until damage prevention completes
  (do-game
    (new-game (default-corp [(qty "Scorched Earth" 2) (qty "Subcontract" 1)])
              (default-runner [(qty "Plascrete Carapace" 1)]))
    (take-credits state :resPlayer)
    (core/gain state :hazPlayer :tag 1)
    (play-from-hand state :hazPlayer "Plascrete Carapace")
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Subcontract")
    (prompt-select :resPlayer (find-card "Scorched Earth" (:hand (get-corp))))
    (is (and (= 1 (count (:prompt (get-corp)))) (= :waiting (-> (get-corp) :prompt first :prompt-type)))
        "Corp does not have Subcontract prompt until damage prevention completes")
    (prompt-choice :hazPlayer "Done")
    (is (not-empty (:prompt (get-corp))) "Corp can now play second Subcontract operation")))

(deftest subcontract-terminal
  ;; Subcontract - interaction with Terminal operations
  (do-game
    (new-game
      (default-corp [(qty "Hard-Hitting News" 2) (qty "Subcontract" 1)])
      (default-runner))
    (core/gain state :hazPlayer :tag 1)
    (take-credits state :resPlayer)
    (run-empty-server state :archives)
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Subcontract")
    (prompt-select :resPlayer (find-card "Hard-Hitting News" (:hand (get-corp))))
    (prompt-choice :resPlayer 0)
    (prompt-choice :hazPlayer 0)
    (is (= 5 (:tag (get-runner))) "Runner has 5 tags")
    (is (empty? (:prompt (get-corp))) "Corp does not have a second Subcontract selection prompt")))

(deftest self-growth-program
  ;; Self-Growth Program - Add 2 installed cards to grip if runner is tagged
  (do-game
    (new-game (default-corp [(qty "Self-Growth Program" 1)])
              (default-runner [(qty "Clone Chip" 1) (qty "Inti" 1)]))
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Clone Chip")
    (play-from-hand state :hazPlayer "Inti")
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Self-Growth Program")
    (is (= 3 (:click (get-corp))) "Self-Growth Program precondition not met; card not played")
    (core/gain state :hazPlayer :tag 1)
    (is (= 0 (count (:hand (get-runner)))) "Runner hand is empty")
    (let [inti (get-in @state [:hazPlayer :rig :program 0])
          cc (get-in @state [:hazPlayer :rig :hardware 0])]
      (play-from-hand state :resPlayer "Self-Growth Program")
      (prompt-select :resPlayer inti)
      (prompt-select :resPlayer cc))
    (is (= 2 (count (:hand (get-runner)))) "2 cards returned to hand")
    (is (= 0 (count (get-in @state [:hazPlayer :rig :program]))) "No programs installed")
    (is (= 0 (count (get-in @state [:hazPlayer :rig :hardware]))) "No hardware installed")))

(deftest service-outage
  ;; Service Outage - First click run each turn costs a credit
  (do-game
    (new-game (default-corp [(qty "Service Outage" 1)])
              (default-runner [(qty "Employee Strike" 1)]))
    (play-from-hand state :resPlayer "Service Outage")
    (take-credits state :resPlayer)

    (is (= 5 (:credit (get-runner))) "Runner has 5 credits")
    (run-on state :archives)
    (is (= 4 (:credit (get-runner)))
        "Runner spends 1 credit to make the first run")
    (run-successful state)

    (run-on state :archives)
    (is (= 4 (:credit (get-runner)))
        "Runner doesn't spend 1 credit to make the second run")
    (run-successful state)

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)

    (core/lose state :hazPlayer :credit 6)
    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (is (= 0 (:credit (get-runner))) "Runner has 0 credits")
    (run-on state :archives)
    (is (not (:run @state)) "No run was initiated")
    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (is (= 0 (:credit (get-runner))) "Runner has 0 credits")

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)

    (core/lose state :hazPlayer :credit 2)
    (play-from-hand state :hazPlayer "Employee Strike")
    (is (= 1 (:credit (get-runner))) "Runner has 1 credit")

    (run-on state :archives)
    (is (= 1 (:credit (get-runner)))
        "Runner doesn't spend 1 credit to make a run")))

(deftest service-outage-card-ability
  ;; Service Outage - First card ability run each turn costs an additional credit
  (do-game
    (new-game (default-corp [(qty "Service Outage" 1)])
              (default-runner [(qty "Sneakdoor Beta" 1)]))
    (play-from-hand state :resPlayer "Service Outage")
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Sneakdoor Beta")
    (take-credits state :hazPlayer 1)

    (is (= 2 (:credit (get-runner))) "Runner has 2 credits")
    (let [sneakdoor (get-in @state [:hazPlayer :rig :program 0])]
      (card-ability state :hazPlayer sneakdoor 0)
      (is (= 1 (:credit (get-runner)))
          "Runner spends 1 additional credit to run with a card ability")
      (run-successful state)

      (run-on state :archives)
      (is (= 1 (:credit (get-runner)))
          "Runner doesn't spend 1 credit to make a run")
      (run-successful state)

      (take-credits state :hazPlayer)
      (take-credits state :resPlayer)

      (core/lose state :hazPlayer :credit 1)
      (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
      (is (= 0 (:credit (get-runner))) "Runner has 0 credits")
      (card-ability state :hazPlayer sneakdoor 0)
      (is (not (:run @state)) "No run was initiated")
      (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
      (is (= 0 (:credit (get-runner))) "Runner has 0 credits"))))

(deftest service-outage-run-events
  ;; Service Outage - First run event each turn costs an additional credit
  (do-game
    (new-game (default-corp [(qty "Service Outage" 1)])
              (default-runner [(qty "Out of the Ashes" 2)]))
    (play-from-hand state :resPlayer "Service Outage")
    (take-credits state :resPlayer)

    (is (= 5 (:credit (get-runner))) "Runner has 5 credits")
    (play-from-hand state :hazPlayer "Out of the Ashes")
    (is (= 3 (:credit (get-runner)))
        "Runner spends 1 additional credit to run with a run event")
    (prompt-choice :hazPlayer "Archives")
    (run-successful state)

    (run-on state :archives)
    (is (= 3 (:credit (get-runner)))
        "Runner doesn't spend 1 credit to make a run")
    (run-successful state)

    (take-credits state :hazPlayer)
    (take-credits state :resPlayer)
    (prompt-choice :hazPlayer "No") ; Out of the Ashes prompt

    (core/lose state :hazPlayer :credit 4)
    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (is (= 1 (:credit (get-runner))) "Runner has 1 credit")
    (play-from-hand state :hazPlayer "Out of the Ashes")
    (is (empty? (get-in @state [:hazPlayer :prompt]))
        "Out of the Ashes was not played")
    (is (= 4 (:click (get-runner))) "Runner has 4 clicks")
    (is (= 1 (:credit (get-runner))) "Runner has 1 credit")))

(deftest service-outage-runner-turn-first-run
  ;; Service Outage - Works when played on the runner's turn
  (do-game
    (new-game (make-deck "New Angeles Sol: Your News" [(qty "Service Outage" 1)
                                                       (qty "Breaking News" 1)])
              (default-runner [(qty "Hades Shard" 1)]))
    (trash-from-hand state :resPlayer "Breaking News")
    (take-credits state :resPlayer)

    (core/gain state :hazPlayer :credit 3)
    (play-from-hand state :hazPlayer "Hades Shard")
    (card-ability state :hazPlayer (get-in @state [:hazPlayer :rig :resource 0]) 0)
    (prompt-choice :hazPlayer "Steal")
    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Service Outage" (:hand (get-corp))))
    (is (find-card "Service Outage" (:current (get-corp)))
        "Service Outage is in play")

    (is (= 1 (:credit (get-runner))) "Runner has 1 credit")
    (run-on state :archives)
    (is (= 0 (:credit (get-runner)))
        "Runner spends 1 additional credit to make a run")))

(deftest service-outage-runner-turn-second-run
  ;; Service Outage - Doesn't fire if already run when played on the runner's turn
  (do-game
    (new-game (make-deck "New Angeles Sol: Your News" [(qty "Service Outage" 1)
                                                       (qty "Breaking News" 1)])
              (default-runner [(qty "Hades Shard" 1)]))
    (trash-from-hand state :resPlayer "Breaking News")
    (take-credits state :resPlayer)

    (run-on state :hq)
    (run-successful state)
    (prompt-choice :hazPlayer "OK")

    (core/gain state :hazPlayer :credit 3)
    (play-from-hand state :hazPlayer "Hades Shard")
    (card-ability state :hazPlayer (get-in @state [:hazPlayer :rig :resource 0]) 0)
    (prompt-choice :hazPlayer "Steal")
    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Service Outage" (:hand (get-corp))))
    (is (find-card "Service Outage" (:current (get-corp)))
        "Service Outage is in play")

    (is (= 1 (:credit (get-runner))) "Runner has 1 credit")
    (run-on state :archives)
    (is (= 1 (:credit (get-runner)))
        "Runner doesn't spend 1 additional credit to make a run")))

(deftest service-outage-new-angeles-sol
  ;; Service Outage trashed and reinstalled on steal doesn't double remove penalty
  (do-game
    (new-game
      (make-deck "New Angeles Sol: Your News" [(qty "Service Outage" 1)
                                               (qty "Breaking News" 1)])
      (default-runner))
    (play-from-hand state :resPlayer "Breaking News" "New remote")
    (play-from-hand state :resPlayer "Service Outage")
    (take-credits state :resPlayer)

    (run-on state :remote1)
    (run-successful state)
    (prompt-choice :hazPlayer "Steal")

    (prompt-choice :resPlayer "Yes")
    (prompt-select :resPlayer (find-card "Service Outage"
                                    (:discard (get-corp))))

    (take-credits state :hazPlayer)

    (take-credits state :resPlayer)

    (is (= 7 (:credit (get-runner))) "Runner has 7 credits")
    (run-on state :archives)
    (is (= 6 (:credit (get-runner)))
        "Runner spends 1 credit to make a run")))

(deftest shipment-from-sansan
  ;; Shipment from SanSan - placing advancements
  (do-game
    (new-game (default-corp [(qty "Shipment from SanSan" 3) (qty "Ice Wall" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Ice Wall" "HQ")
    (let [iwall (get-ice state :hq 0)]
      (play-from-hand state :resPlayer "Shipment from SanSan")
      (prompt-choice :resPlayer "2")
      (prompt-select :resPlayer iwall)
      (is (= 5 (:credit (get-corp))))
      (is (= 2 (:advance-counter (refresh iwall)))))))

(deftest stock-buy-back
  ;; Stock Buy-Back - Gain 3c for every agenda in Runner's area
  (do-game
    (new-game (default-corp [(qty "Hostile Takeover" 2) (qty "Stock Buy-Back" 3)])
              (default-runner))
    (play-from-hand state :resPlayer "Hostile Takeover" "New remote")
    (play-from-hand state :resPlayer "Hostile Takeover" "New remote")
    (take-credits state :resPlayer)
    (run-empty-server state "Server 1")
    (prompt-choice :hazPlayer "Steal")
    (run-empty-server state "Server 2")
    (prompt-choice :hazPlayer "Steal")
    (take-credits state :hazPlayer)
    (is (= 2 (count (:scored (get-runner)))))
    (play-from-hand state :resPlayer "Stock Buy-Back")
    (is (= 11 (:credit (get-corp))))))

(deftest sub-boost
  ;; Sub Boost - Give ICE Barrier
  (do-game
    (new-game (default-corp [(qty "Sub Boost" 1) (qty "Quandary" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Quandary" "HQ")
    (play-from-hand state :resPlayer "Sub Boost")
    (let [qu (get-ice state :hq 0)]
      (core/rez state :resPlayer qu)
      (prompt-select :resPlayer qu)
      (is (core/has-subtype? (refresh qu) "Code Gate") "Quandary has Code Gate")
      (is (core/has-subtype? (refresh qu) "Barrier") "Quandary ICE Barrier"))))

(deftest subliminal-messaging
  ;; Subliminal Messaging - Playing/trashing/milling will all prompt returning to hand
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 3)])
              (make-deck "Noise: Hacker Extraordinaire" [(qty "Cache" 3) (qty "Utopia Shard" 1)]))
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (is (= 6 (:credit (get-corp))))
    (is (= 3 (:click (get-corp))) "First Subliminal Messaging gains 1 click")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (is (= 7 (:credit (get-corp))))
    (is (= 2 (:click (get-corp))) "Second Subliminal Messaging does not gain 1 click")
    (trash-from-hand state :resPlayer "Subliminal Messaging")
    (is (= 0 (count (:hand (get-corp)))))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (is (= 3 (count (:hand (get-corp)))) "All 3 Subliminals returned to HQ")
    (core/move state :resPlayer (find-card "Subliminal Messaging" (:hand (get-corp))) :deck)
    (take-credits state :resPlayer)
    (play-from-hand state :hazPlayer "Cache")
    (play-from-hand state :hazPlayer "Utopia Shard")
    (let [utopia (get-in @state [:hazPlayer :rig :resource 0])]
      (card-ability state :hazPlayer utopia 0))
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (is (= 3 (count (:hand (get-corp)))) "All 3 Subliminals returned to HQ")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (take-credits state :resPlayer)
    (run-on state "R&D")
    (run-jack-out state)
    (take-credits state :hazPlayer)
    (is (empty? (get-in @state [:resPlayer :prompt])) "No prompt here because runner made a run last turn")
    (take-credits state :resPlayer)
    (is (= 2 (count (:hand (get-corp)))))
    (is (= 1 (count (:discard (get-corp)))) "1 Subliminal not returned because runner made a run last turn")))

(deftest subliminal-messaging-archived
  ;; Subliminal Messaging - Scenario involving Subliminal being added to HQ with Archived Memories
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 2) (qty "Archived Memories" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Archived Memories")
    (prompt-select :resPlayer (find-card "Subliminal Messaging" (:discard (get-corp))))
    (is (= 2 (count (:discard (get-corp)))))
    (is (= 1 (count (:hand (get-corp)))))
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "No")
    (is (empty? (get-in @state [:resPlayer :prompt])) "Only 1 Subliminal prompt")
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (is (empty? (get-in @state [:resPlayer :prompt]))
        "Only 2 Subliminal prompts - there will be a third if flag not cleared")))

(deftest subliminal-messaging-jackson
  ;; Subliminal Messaging - Scenario involving Subliminal being reshuffled into R&D with Jackson
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 1) (qty "Jackson Howard" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (play-from-hand state :resPlayer "Jackson Howard" "New remote")
    (take-credits state :resPlayer)
    (let [jhow (get-content state :remote1 0)]
      (core/rez state :resPlayer jhow)
      (card-ability state :resPlayer jhow 1)
      (prompt-select :resPlayer (find-card "Subliminal Messaging" (:discard (get-corp))))
      (prompt-choice :resPlayer "Done")
      (is (= 0 (count (:discard (get-corp)))))
      (is (= 1 (count (:rfg (get-corp))))))
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "Yes")
    (is (= 1 (count (:hand (get-corp)))) "Subliminal returned to HQ")
    (is (empty? (get-in @state [:resPlayer :prompt]))
        "Subliminal prompt cleared - there will be a second prompt if flag not cleared")))

(deftest subliminal-messaging-made-run
  ;; Subliminal Messaging - Runner made run, ensure game asks again next turn
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 2)])
              (default-runner))
  (play-from-hand state :resPlayer "Subliminal Messaging")
  (trash-from-hand state :resPlayer "Subliminal Messaging")
  (take-credits state :resPlayer)
  (run-on state "R&D")
  (run-jack-out state)
  (take-credits state :hazPlayer)
  (is (empty? (get-in @state [:resPlayer :prompt])) "No prompt here because runner made a run last turn")
  (take-credits state :resPlayer)
  (take-credits state :hazPlayer)
  (prompt-choice :resPlayer "Yes")
  (prompt-choice :resPlayer "Yes")
  (is (= 2 (count (:hand (get-corp)))) "Both Subliminals returned to HQ")
  (is (= 0 (count (:discard (get-corp)))) "No Subliminals in Archives")))

(deftest subliminal-messaging-no
  ;; Subliminal Messaging - User declines to return to hand, ensure game asks again next turn
  (do-game
    (new-game (default-corp [(qty "Subliminal Messaging" 2)])
              (default-runner))
    (play-from-hand state :resPlayer "Subliminal Messaging")
    (trash-from-hand state :resPlayer "Subliminal Messaging")
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "No")
    (prompt-choice :resPlayer "No")
    (is (= 0 (count (:hand (get-corp)))) "Neither Subliminal returned to HQ")
    (is (= 2 (count (:discard (get-corp)))) "Both Subliminals in Archives")
    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)
    (prompt-choice :resPlayer "Yes")
    (prompt-choice :resPlayer "Yes")
    (is (= 2 (count (:hand (get-corp)))) "Both Subliminals returned to HQ")
    (is (= 0 (count (:discard (get-corp)))) "No Subliminals in Archives")))

(deftest success-bad-publicity
  ;; Success - Works with bad publicity
  (do-game
    (new-game (default-corp [(qty "NAPD Contract" 1) (qty "Project Beale" 1) (qty "Success" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "NAPD Contract" "New remote")
    (play-from-hand state :resPlayer "Project Beale" "New remote")
    (core/gain state :resPlayer :bad-publicity 9)
    (core/gain state :resPlayer :credit 8)
    (core/gain state :resPlayer :click 15)
    (let [napd (get-content state :remote1 0)
          beale (get-content state :remote2 0)]
      (dotimes [_ 13] (core/advance state :resPlayer {:card (refresh napd)}))
      (is (= 13 (:advance-counter (refresh napd))))
      (core/score state :resPlayer {:card (refresh napd)})
      (is (= 2 (:agenda-point (get-corp))))
      (play-from-hand state :resPlayer "Success")
      (prompt-select :resPlayer (get-scored state :resPlayer 0))
      (is (= "NAPD Contract" (:title (first (:rfg (get-corp))))))
      (prompt-select :resPlayer (refresh beale))
      (is (= 13 (:advance-counter (refresh beale))))
      (core/score state :resPlayer {:card (refresh beale)})
      (is (= 7 (:agenda-point (get-corp)))))))

(deftest success-public-agenda
  ;; Success - Works with public agendas
  (do-game
    (new-game (default-corp [(qty "Oaktown Renovation" 1) (qty "Vanity Project" 1) (qty "Success" 1)])
              (default-runner))
    (core/gain state :resPlayer :click 1)
    (score-agenda state :resPlayer (find-card "Vanity Project" (:hand (get-corp))))
    (is (= 4 (:agenda-point (get-corp))))
    (play-from-hand state :resPlayer "Oaktown Renovation" "New remote")
    (is (= 5 (:credit (get-corp))))
    (play-from-hand state :resPlayer "Success")
    (prompt-select :resPlayer (get-scored state :resPlayer))
    (is (= "Vanity Project" (:title (first (:rfg (get-corp))))))
    (let [oaktown (get-content state :remote1 0)]
      (prompt-select :resPlayer (refresh oaktown))
      (is (= 6 (:advance-counter (refresh oaktown))))
      (is (= 19 (:credit (get-corp))) "Gain 2 + 2 + 2 + 2 + 3 + 3 = 14 credits for advancing Oaktown")
      (core/score state :resPlayer {:card (refresh oaktown)})
      (is (= 2 (:agenda-point (get-corp)))))))

(deftest success-jemison
  ;; Success interaction with Jemison, regression test for issue #2704
  (do-game
    (new-game (make-deck "Jemison Astronautics: Sacrifice. Audacity. Success."
                         [(qty "Success" 1)
                          (qty "High-Risk Investment" 1)
                          (qty "Government Takeover" 1)])
              (default-runner))
    (core/gain state :resPlayer :click 1)
    (score-agenda state :resPlayer (find-card "High-Risk Investment" (:hand (get-corp))))
    (play-from-hand state :resPlayer "Government Takeover" "New remote")
    (play-from-hand state :resPlayer "Success")
    (prompt-select :resPlayer (get-in (get-corp) [:scored 0]))
    (let [gto (get-content state :remote1 0)]
      ;; Prompt for Success
      (prompt-select :resPlayer (refresh gto))
      (is (= 5 (:advance-counter (refresh gto))) "Advance 5 times from Success")
      ;; Prompt for Jemison
      (prompt-select :resPlayer (refresh gto))
      (is (= 9 (:advance-counter (refresh gto))) "Added 4 counters from Jemison trigger"))))

(deftest successful-demonstration
  ;; Successful Demonstration - Play if only Runner made unsuccessful run last turn; gain 7 credits
  (do-game
    (new-game (default-corp [(qty "Successful Demonstration" 1)])
              (default-runner))
    (play-from-hand state :resPlayer "Successful Demonstration")
    (is (and (= 3 (:click (get-corp)))
             (= 5 (:credit (get-runner))))
        "Successful Demonstration precondition not met; card not played")
    (take-credits state :resPlayer)
    (run-on state "R&D")
    (run-jack-out state)
    (take-credits state :hazPlayer)
    (play-from-hand state :resPlayer "Successful Demonstration")
    (is (= 13 (:credit (get-corp))) "Paid 2 to play event; gained 7 credits")))

(deftest threat-assessment
  ;; Threat Assessment - play only if runner trashed a card last turn, move a card to the stack or take 2 tags
  (do-game
    (new-game (default-corp [(qty "Threat Assessment" 3) (qty "Adonis Campaign" 1)])
              (default-runner [(qty "Desperado" 1) (qty "Corroder" 1)]))
    (play-from-hand state :resPlayer "Adonis Campaign" "New remote")
    (take-credits state :resPlayer)

    (run-on state :remote1)
    (run-successful state)
    (prompt-choice :hazPlayer "Yes") ;trash
    (core/gain state :hazPlayer :credit 5)
    (play-from-hand state :hazPlayer "Desperado")
    (play-from-hand state :hazPlayer "Corroder")
    (take-credits state :hazPlayer)

    (is (= 0 (:tag (get-runner))) "Runner starts with 0 tags")
    (play-from-hand state :resPlayer "Threat Assessment")
    (prompt-select :resPlayer (find-card "Desperado" (-> (get-runner) :rig :hardware)))
    (prompt-choice :hazPlayer "2 tags")
    (is (= 2 (:tag (get-runner))) "Runner took 2 tags")
    (is (= 1 (count (-> (get-runner) :rig :hardware))) "Didn't trash Desperado")
    (is (= "Threat Assessment" (:title (first (:rfg (get-corp))))) "Threat Assessment removed from game")

    (play-from-hand state :resPlayer "Threat Assessment")
    (prompt-select :resPlayer (find-card "Corroder" (-> (get-runner) :rig :program)))
    (prompt-choice :hazPlayer "Move Corroder")
    (is (= 2 (:tag (get-runner))) "Runner didn't take tags")
    (is (= "Corroder" (:title (first (:deck (get-runner))))) "Moved Corroder to the deck")
    (is (= 2 (count (:rfg (get-corp)))))
    (take-credits state :hazPlayer)

    (take-credits state :resPlayer)
    (take-credits state :hazPlayer)

    (play-from-hand state :resPlayer "Threat Assessment")
    (is (empty? (:prompt (get-corp))) "Threat Assessment triggered with no trash")))

(deftest transparency-initiative
  ;; Transparency Initiative - Full test
  (do-game
    (new-game (default-corp [(qty "Transparency Initiative" 1) (qty "Oaktown Renovation" 1)
                             (qty "Project Atlas" 1) (qty "Hostile Takeover" 1) (qty "Casting Call" 1)])
              (default-runner))
    (core/gain state :resPlayer :click 5)
    (play-from-hand state :resPlayer "Oaktown Renovation" "New remote")
    (play-from-hand state :resPlayer "Casting Call")
    (prompt-select :resPlayer (find-card "Project Atlas" (:hand (get-corp))))
    (prompt-choice :resPlayer "New remote")
    (play-from-hand state :resPlayer "Hostile Takeover" "New remote")
    (let [oaktown (get-content state :remote1 0)
          atlas (get-content state :remote2 0)
          hostile (get-content state :remote3 0)]
      (play-from-hand state :resPlayer "Transparency Initiative")
      (prompt-select :resPlayer (refresh oaktown))
      ;; doesn't work on face-up agendas
      (is (= 0 (count (:hosted (refresh oaktown)))))
      (prompt-select :resPlayer (refresh atlas))
      (is (= 1 (count (:hosted (refresh atlas)))) "Casting Call")
      ;; works on facedown agenda
      (prompt-select :resPlayer (refresh hostile))
      (is (= 1 (count (:hosted (refresh hostile)))))
      ;; gains Public subtype
      (is (core/has-subtype? (refresh hostile) "Public"))
      ;; gain 1 credit when advancing
      (is (= 5 (:credit (get-corp))))
      (core/advance state :resPlayer {:card (refresh hostile)})
      (is (= 5 (:credit (get-corp))))
      ;; make sure advancing other agendas doesn't gain 1
      (core/advance state :resPlayer {:card (refresh oaktown)})
      (is (= 6 (:credit (get-corp))) "Transparency initiative didn't fire")
      (core/advance state :resPlayer {:card (refresh atlas)})
      (is (= 5 (:credit (get-corp))) "Transparency initiative didn't fire"))))

(deftest wetwork-refit
  ;; Wetwork Refit - Only works on Bioroid ICE and adds a subroutine
  (do-game
    (new-game (default-corp [(qty "Eli 1.0" 1)
                             (qty "Vanilla" 1)
                             (qty "Wetwork Refit" 3)])
              (default-runner))
    (core/gain state :resPlayer :credit 20)
    (core/gain state :resPlayer :click 10)
    (play-from-hand state :resPlayer "Eli 1.0" "R&D")
    (play-from-hand state :resPlayer "Vanilla" "HQ")
    (let [eli (get-ice state :rd 0)
          vanilla (get-ice state :hq 0)]
      (play-from-hand state :resPlayer "Wetwork Refit")
      (is (not-any? #{"Eli 1.0"} (get-in @state [:resPlayer :prompt :choices]))
          "Unrezzed Eli 1.0 is not a choice to host Wetwork Refit")
      (prompt-choice :resPlayer "Done")

      (take-credits state :resPlayer)
      (take-credits state :hazPlayer)
      (core/rez state :resPlayer (refresh eli))
      (core/rez state :resPlayer (refresh vanilla))

      (play-from-hand state :resPlayer "Wetwork Refit")
      (prompt-select :resPlayer (refresh eli))
      (is (= "Wetwork Refit" (:title (first (:hosted (refresh eli)))))
          "Wetwork Refit is hosted on Eli 1.0")
      (is (= 2 (count (:subroutines (refresh eli))))
          "Eli 1.0 has 2 different subroutines")
      (is (= "Do 1 brain damage" (:label (first (:subroutines (refresh eli)))))
          "Eli 1.0 has a brain damage subroutine as his first subroutine")

      (core/move state :resPlayer (first (:hosted (refresh eli))) :hand)
      (is (empty? (:hosted (refresh eli))) "No cards are hosted on Eli 1.0")
      (is (= 1 (count (:subroutines (refresh eli))))
          "Eli 1.0 has 1 different subroutine")
      (is (= "End the run" (:label (first (:subroutines (refresh eli)))))
          "Eli 1.0 has an end the run subroutine as his first subroutine")

      (play-from-hand state :resPlayer "Wetwork Refit")
      (prompt-select :resPlayer (refresh vanilla))
      (is (not= "Wetwork Refit" (:title (first (:hosted (refresh vanilla)))))
          "Wetwork Refit is not hosted on Vanilla"))))
