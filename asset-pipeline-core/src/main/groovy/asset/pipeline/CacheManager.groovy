/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package asset.pipeline

/**
 * Simple cache manager for Asset Pipeline
 *
 * @author David Estes
 * @author Graeme Rocher
 */
class CacheManager {
	static Map<String, Map<String, Object>> cache = [:]

	static String findCache(String fileName, String md5, String originalFileName = null) {
		def cacheRecord = cache[fileName]

		if(cacheRecord && cacheRecord.md5 == md5 && cacheRecord.originalFileName == originalFileName) {
			def cacheFiles = cacheRecord.dependencies.keySet()
			def expiredCacheFound = cacheFiles.find { String cacheFileName ->
				def cacheFile = AssetHelper.fileForUri(cacheFileName)
				if(!cacheFile)
					return true
				def depMd5 = AssetHelper.getByteDigest(cacheFile.inputStream.bytes)
				if(cacheRecord.dependencies[cacheFileName] != depMd5) {
					return true
				}
				return false
			}

			if(expiredCacheFound) {
				cache.remove(fileName)
				return null
			}
			return cacheRecord.processedFileText
		} else if (cacheRecord) {
			cache.remove(fileName)
			return null
		}
	}

	static void createCache(String fileName, String md5Hash, String processedFileText, String originalFileName = null) {
        def thisCache = cache
        def cacheRecord = thisCache[fileName]
		if(cacheRecord) {
			thisCache[fileName] = cacheRecord + [
				md5: md5Hash,
				originalFileName: originalFileName,
				processedFileText: processedFileText
			]
		} else {
			thisCache[fileName] = [
				md5: md5Hash,
				originalFileName: originalFileName,
				processedFileText: processedFileText,
				dependencies: [:]
			]
		}

	}

	static void addCacheDependency(String fileName, AssetFile dependentFile) {
		def cacheRecord = cache[fileName]
		if(!cacheRecord) {
			createCache(fileName, null, null)
			cacheRecord = cache[fileName]
		}
		def newMd5 = AssetHelper.getByteDigest(dependentFile.inputStream.bytes)
		cacheRecord.dependencies[dependentFile.path] = newMd5
	}
}
