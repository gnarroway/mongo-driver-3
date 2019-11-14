(ns mongo-driver-3.operator
  "Define constants corresponding to mongo operators")

(defmacro defoperator
  [op]
  `(def ^{:const true} ~op ~(str op)))

;;; --- Query operators
;;; https://docs.mongodb.com/manual/reference/operator/query/

;;; Comparison

(defoperator $eq)
(defoperator $gt)
(defoperator $gte)
(defoperator $in)
(defoperator $lt)
(defoperator $lte)
(defoperator $ne)
(defoperator $nin)

;;; Logical

(defoperator $and)
(defoperator $not)
(defoperator $nor)
(defoperator $or)

;;; Element

(defoperator $exists)
(defoperator $type)

;;; Evaluation

(defoperator $expr)
(defoperator $jsonSchema)
(defoperator $mod)
(defoperator $regex)
(defoperator $text)
(defoperator $where)

;;; Geospatial

(defoperator $geoIntersects)
(defoperator $geoWithin)
(defoperator $near)
(defoperator $geoSphere)

;;; Array

(defoperator $all)
(defoperator $elemMatch)
(defoperator $size)

;;; Bitwise

(defoperator $bitsAllClear)
(defoperator $bitsAllSet)
(defoperator $bitsAnyClear)
(defoperator $bitsAnySet)

;;; Comments

(defoperator $comment)

;;; Projection

(defoperator $)
(defoperator $meta)
(defoperator $slice)

;;; --- Update operators
;;; https://docs.mongodb.com/manual/reference/operator/update/

;;; Fields

(defoperator $currentDate)
(defoperator $inc)
(defoperator $min)
(defoperator $max)
(defoperator $mul)
(defoperator $rename)
(defoperator $set)
(defoperator $setOnInsert)
(defoperator $unset)

;;; Array operators

(defoperator $addToSet)
(defoperator $pop)
(defoperator $pull)
(defoperator $push)
(defoperator $pushAll)

;;; Array modifiers

(defoperator $each)
(defoperator $position)
(defoperator $slice)
(defoperator $sort)

;;; Bitwise

(defoperator $bit)

;;; --- Aggregation stages
;;; https://docs.mongodb.com/manual/reference/operator/aggregation-pipeline/

(defoperator $addFields)
(defoperator $bucket)
(defoperator $bucketAuto)
(defoperator $collStats)
(defoperator $count)
(defoperator $currentOp)
(defoperator $facet)
(defoperator $geoNear)
(defoperator $graphLookup)
(defoperator $group)
(defoperator $indexStats)
(defoperator $limit)
(defoperator $listLocalSessions)
(defoperator $listSessions)
(defoperator $lookup)
(defoperator $match)
(defoperator $merge)
(defoperator $out)
(defoperator $planCacheStats)
(defoperator $project)
(defoperator $redact)
(defoperator $replaceRoot)
(defoperator $replaceWith)
(defoperator $sample)
(defoperator $set)
(defoperator $skip)
(defoperator $sort)
(defoperator $sortByCount)
(defoperator $unset)
(defoperator $unwind)


;;; --- Aggregation operators
;;; https://docs.mongodb.com/manual/reference/operator/aggregation/


(defoperator $abs)
(defoperator $acos)
(defoperator $acosh)
(defoperator $add)
(defoperator $addToSet)
(defoperator $allElementsTrue)
(defoperator $and)
(defoperator $anyElementTrue)
(defoperator $arrayElemAt)
(defoperator $arrayToObject)
(defoperator $asin)
(defoperator $asinh)
(defoperator $atan)
(defoperator $atan2)
(defoperator $atanh)
(defoperator $avg)
(defoperator $ceil)
(defoperator $cmp)
(defoperator $concat)
(defoperator $concatArrays)
(defoperator $cond)
(defoperator $convert)
(defoperator $cos)
(defoperator $dateFromParts)
(defoperator $dateToParts)
(defoperator $dateFromString)
(defoperator $dateToString)
(defoperator $dayOfMonth)
(defoperator $dayOfWeek)
(defoperator $dayOfYear)
(defoperator $degreesToRadians)
(defoperator $divide)
(defoperator $eq)
(defoperator $exp)
(defoperator $filter)
(defoperator $first)
(defoperator $floor)
(defoperator $gt)
(defoperator $gte)
(defoperator $hour)
(defoperator $ifNull)
(defoperator $in)
(defoperator $indexOfArray)
(defoperator $indexOfBytes)
(defoperator $indexOfCP)
(defoperator $isArray)
(defoperator $isoDayOfWeek)
(defoperator $isoWeek)
(defoperator $isoWeekYear)
(defoperator $last)
(defoperator $let)
(defoperator $literal)
(defoperator $ln)
(defoperator $log)
(defoperator $log10)
(defoperator $lt)
(defoperator $lte)
(defoperator $ltrim)
(defoperator $map)
(defoperator $max)
(defoperator $mergeObjects)
(defoperator $meta)
(defoperator $min)
(defoperator $millisecond)
(defoperator $minute)
(defoperator $mod)
(defoperator $month)
(defoperator $multiply)
(defoperator $ne)
(defoperator $not)
(defoperator $objectToArray)
(defoperator $or)
(defoperator $pow)
(defoperator $push)
(defoperator $radiansToDegrees)
(defoperator $range)
(defoperator $reduce)
(defoperator $regexFind)
(defoperator $regexFindAll)
(defoperator $regexMatch)
(defoperator $reverseArray)
(defoperator $round)
(defoperator $rtrim)
(defoperator $second)
(defoperator $setDifference)
(defoperator $setEquals)
(defoperator $setIntersection)
(defoperator $setIsSubset)
(defoperator $setUnion)
(defoperator $size)
(defoperator $sin)
(defoperator $slice)
(defoperator $split)
(defoperator $sqrt)
(defoperator $stdDevPop)
(defoperator $stdDevSamp)
(defoperator $strcasecmp)
(defoperator $strLenBytes)
(defoperator $strLenCP)
(defoperator $substr)
(defoperator $substrBytes)
(defoperator $substrCP)
(defoperator $subtract)
(defoperator $sum)
(defoperator $switch)
(defoperator $tan)
(defoperator $toBool)
(defoperator $toDate)
(defoperator $toDecimal)
(defoperator $toDouble)
(defoperator $toInt)
(defoperator $toLong)
(defoperator $toObjectId)
(defoperator $toString)
(defoperator $toLower)
(defoperator $toUpper)
(defoperator $trim)
(defoperator $trunc)
(defoperator $type)
(defoperator $week)
(defoperator $year)
(defoperator $zip)

;;; --- Query modifiers
;;; https://docs.mongodb.com/manual/reference/operator/query-modifier/

(defoperator $comment)
(defoperator $explain)
(defoperator $hint)
(defoperator $max)
(defoperator $maxTimeMS)
(defoperator $min)
(defoperator $orderby)
(defoperator $query)
(defoperator $returnKey)
(defoperator $showDiskLoc)
(defoperator $natural)