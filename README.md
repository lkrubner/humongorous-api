# humongorous-api

humongorous-api  provides endpoints to our front Javascript to enable all CRUD operations via Ajax. It also provides some minimal data conversions, for instance, one of the frontenders wanted to save JSON objects with field names such as $$hashKey, and dollar signs are reserved in MongoDB, so we convert $ to * when saving and we convert * to $ when we are fetching.

## Our RESTful Philosophy

For our purposes, CRUD maps to HTTP verbs like this: 

Create = 

    PUT (if there is a document _id in the document -- the old document is completely overwritten)

    POST (if there is NOT a document _id in the document)

Retrieve = GET

Update = POST (there MUST be a document _id in the document, and the new content will be merged with old)

Delete = DELETE (there is MUST be a document _id in the document, and the document will be deleted)

If you need to know more about CRUD to REST mapping, read here:

http://jcalcote.wordpress.com/2008/10/16/put-or-post-the-rest-of-the-story/


## Usage

We expect a collection name, at a minimum. 

This returns the first 10 items in the CourseActivityGrade collection:

GET

http://localhost/v0.2/CourseActivityGrade

The return would look like this (only 2 items shown for the sake of brevity):

    [
    {
	_id : ObjectId("53a484c03d698858278c2198"),
	accountID : "doh",
	id : "55-67-4846",
	finalgrade : "56.00",
	userid : "55"
    },
    {
	_id : ObjectId("53a484c03d698858278c2199"),
	accountID : "doh",
	id : "55-67-4845",
	finalgrade : "78.00",
	userid : "55"
    }
    ]

If you append a document id:

GET

http://localhost/v0.2/CourseActivityGrade/53a484c03d698858278c219c

Then you get that document:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	accountID : "doh",
	id : "13-67-4846",
	finalgrade : "85.00",
	userid : "13"
    }

If you add the "sort" parameter, then you can add 3 more parameters: a sort field, a starting document (offset), and how many you want (limit). So this:

http://localhost/v0.2/CourseActivityGrade/sort/finalgrade/400/1000

will sort by "finalgrade", then skip over the first 400 documents, and then give you 1,000 documents.

If you then POST an attribute and value { "country_of_origin" : "Poland" } using this document id: 

POST

http://localhost/v0.2/CourseActivityGrade/53a484c03d698858278c219c

the data is then merged with the existing document:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	accountID : "doh",
	id : "13-67-4846",
	finalgrade : "85.00",
	userid : "13",
	country_of_origin : "Poland"
    }

However, if you use PUT instead of POST, then the document is over-written with the new data:

PUT

http://localhost/v0.2/CourseActivityGrade/53a484c03d698858278c219c

gives you:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	country_of_origin : "Poland"
    }

You can also use PUT to create a new document. Here we assume you are sending this data with no document id: 

    {
	"accountID" : "doh",
	"id" : "55-67-4847",
	"finalgrade" : "85.00",
	"userid" : "55"
    }

So this with the above payload:

PUT

http://localhost/v0.2/CourseActivityGrade/53a484c03d698858278c219c

means this document is now in the database:

    {
	"_id" : ObjectId("53a484c03d698858278c2197"),
	"accountID" : "doh",
	"id" : "55-67-4847",
	"finalgrade" : "85.00",
	"userid" : "55"
    }

Finally, if you DELETE with a document id, then the document is deleted: 

DELETE

http://localhost/v0.2/CourseActivityGrade/53a484c03d698858278c219c





## Setup

Configuration is kept in /etc/humongorous, and should looks like this:

{
 :host "localhost
 :db "database"
 :username "user"
 :password "1234"
}


To compile this app, you will need to have the JVM installed on your computer. Also, be sure you have Leinengen installed on your computer:

http://leiningen.org/

Once you have that installed, you can cd to the directory where this project is, and then, at the command prompt, type:

lein uberjar

You can the start the app by cd'ing to the directory where the binary is and running: 

java -jar humongorous-api-0.1-standalone.jar 40000

That starts the app on port 40000. You can specify any port you want. If you don't already have an app listening on port 80, you can run this on port 80. If you forget to specify a port, the app defaults to port 34000. 


## Testing with cURL

On my own machine, I start the app like this:

java -jar humongorous-api-0.1-standalone.jar

Now it is running on port 34000

Now I go here to get a valid token: 

curl http://localhost:34000/token

This is returned to me:

{ token: 60d77773-592d-4817-afe4-b1033a23271d }

I will use this token in all of my following curl calls. 

If I do this I can add a document to the Mongo database, the "User" collection: 

 curl -X PUT l -d '{ "firstname":"lawrence", "lastname":"krubner", "country-of-origin":"USA"  }'  -H "Content-Type:application/json" http://localhost:34000/v0.2/60d77773-592d-4817-afe4-b1033a23271d/User

This shows me the whole contents of the User collection: 

curl http://localhost:34000/v0.2/60d77773-592d-4817-afe4-b1033a23271d/User

If I then take one of the "id"s that I see, and use in the URL, I can delete a document like this:

curl -X DELETE http://localhost:34000/v0.2/60d77773-592d-4817-afe4-b1033a23271d/User/546049980364672bba8e0e98

So now I have deleted the 53a484cd3d698858278c3baa document from the Assignments collection.


## Our Design Philosophy

I am influenced by "design by contract" and I am moving toward the idea of optional typing. For now, the important functions have both pre and post assertions. For instance, the database function that paginates results (allows a limit and offset) defines 8 pre assertions, and 1 post assertion. These assertions partly take the place of unit tests, and they clearly tell all future developers what this function is expecting. (The assertions slow the code and so they are only used in development. The compiler accepts a flag that strips out all of the assertions when we are ready to move to production.)

    (defn paginate-results [ctx]
    {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :name-of-collection])) 
         (string? (get-in ctx [:request :field-to-sort-by])) 
         (string? (get-in ctx [:request :offset-by-how-many])) 
         (string? (get-in ctx [:request :return-how-many])) 
         (number? (Integer/parseInt (get-in ctx [:request :offset-by-how-many])))
         (number? (Integer/parseInt (get-in ctx [:request :return-how-many]))) 
         ]
    :post [(= (type %) clojure.lang.LazySeq)]}
    (with-collection (get-in ctx [:request :name-of-collection])
    (find (:database-where-clause-map ctx))
    (sort (array-map  (get-in ctx [:request :field-to-sort-by]) 1))
    (limit (Integer/parseInt (get-in ctx [:request :return-how-many]))
    (skip (Integer/parseInt (get-in ctx [:request :offset-by-how-many]))))))




## Runtime optimizations

As said above, we use :pre and :post assertions during development, which partly take the place of unit tests, and partly act as documentation for future developers. However, they slow down the app, and they should be removed when we go to production. *assert* must be set to false, to let the compiler know it should strip out all of the assertions. We want this in the project.clj when we got to production: 

:global-vars {*warn-on-reflection* false
                *assert* false}



## Daemonization

An example: I run this app by putting a script like this in /etc/init.d/ :


#!/bin/sh
### BEGIN INIT INFO
# Provides: humongorous_api
# X-Interactive: true
# Short-Description: Start/stop humongorous_api server
### END INIT INFO

WORK_DIR=”/home/dega”
NAME=”humongorous_api”
JAR=”humongorous_api-0.1-standalone.jar”
USER=”dega”
DAEMON=”/usr/bin/java”
DAEMON_ARGS=” -jar $WORK_DIR/$JAR ”

#export HUMONGOROUS_API_TOKEN=””

start () {
echo “Starting humongorous_api…”
if [ ! -f $WORK_DIR/humongorous_api.pid ]; then
start-stop-daemon –start –verbose –background –chdir $WORK_DIR –exec $DAEMON –pidfile $WORK_DIR/humongorous_api.pid –chuid “$USER” –make-pidfile — $DAEMON_ARGS
else
echo “humongorous_api is already running…”
fi
}

stop () {
echo “Stopping humongorous_api…”
start-stop-daemon –stop –exec $DAEMON –pidfile $WORK_DIR/humongorous_api.pid
rm $WORK_DIR/humongorous_api.pid
}

case $1 in
start)
start
;;
stop)
stop
;;
restart)
stop
start
;;
esac