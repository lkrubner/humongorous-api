(ns humongorous-api.problems)


;; this establishes a hierarchy of error messages. For instance:
;; ::database-logging ::logging
;; so a database-logging problem is a child of a logging. If we don't
;; catch database-logging problems, we can simply catch logging

(derive :humongorous-api.problems/channel-problem :humongorous-api.problems/problem)
(derive :humongorous-api.problems/persistence-channel-closed :humongorous-api.problems/channel-problem)
(derive :humongorous-api.problems/persistence-channel-drained :humongorous-api.problems/channel-problem)
(derive :humongorous-api.problems/unable-to-connect-to-database :humongorous-api.problems/database-problem)  
(derive :humongorous-api.problems/datbase-problem :humongorous-api.problems/problem)
(derive :humongorous-api.problems/no-config-file :humongorous-api.problems/startup-problem)
(derive :humongorous-api.problems/no-log-file :humongorous-api.problems/startup-problem)
(derive :humongorous-api.problems/startup-problem :humongorous-api.problems/problem)
(derive :humongorous-api.problems/database-logging :humongorous-api.problems/logging)





