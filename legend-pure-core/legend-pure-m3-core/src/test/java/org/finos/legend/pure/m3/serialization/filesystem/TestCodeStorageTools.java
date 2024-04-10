// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m3.serialization.filesystem;

import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCodeStorageTools
{
    @Test
    public void testIsRootPath()
    {
        Assertions.assertTrue(CodeStorageTools.isRootPath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertTrue(CodeStorageTools.isRootPath("/"));

        Assertions.assertFalse(CodeStorageTools.isRootPath(null));
        Assertions.assertFalse(CodeStorageTools.isRootPath(""));
        Assertions.assertFalse(CodeStorageTools.isRootPath("/definitely/not/the/root/path"));
        Assertions.assertFalse(CodeStorageTools.isRootPath("not even a valid path"));
        Assertions.assertFalse(CodeStorageTools.isRootPath("root"));
        Assertions.assertFalse(CodeStorageTools.isRootPath("root.pure"));
    }

    @Test
    public void testIsValidPath()
    {
        Assertions.assertTrue(CodeStorageTools.isValidPath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertTrue(CodeStorageTools.isValidPath(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertTrue(CodeStorageTools.isValidPath(WelcomeCodeStorage.WELCOME_FILE_NAME));
        Assertions.assertTrue(CodeStorageTools.isValidPath("platform"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/platform"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/platform/pure/corefunctions/"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("nonexistent/but/still/valid"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid/"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.pure"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.csv"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.css"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.html"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.js"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/nonexistent/but/still/valid.json"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/Valid/path/Including/_somE_/capITAL/LETTERS_and_UNDERscoreS.txt"));
        Assertions.assertTrue(CodeStorageTools.isValidPath("/v4l1d/p4th/1nclud1ng/numb3rs.num"));

        Assertions.assertFalse(CodeStorageTools.isValidPath(null));
        Assertions.assertFalse(CodeStorageTools.isValidPath(""));
        Assertions.assertFalse(CodeStorageTools.isValidPath("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/path/with/special/$#/chars"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/path/with/an/empty//element"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("//"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("//another/path/with/an/empty/element"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/path/with/empty/file/extension."));
        Assertions.assertFalse(CodeStorageTools.isValidPath("empty_file_extension."));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/invalid/file/extension.-abc"));
        Assertions.assertFalse(CodeStorageTools.isValidPath("/missing/file/name/.pure"));
    }

    @Test
    public void testIsValidFilePath()
    {
        Assertions.assertTrue(CodeStorageTools.isValidFilePath(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath(WelcomeCodeStorage.WELCOME_FILE_NAME));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.csv"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.css"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.html"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.js"));
        Assertions.assertTrue(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/file.json"));

        Assertions.assertFalse(CodeStorageTools.isValidFilePath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/platform/pure/corefunctions"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/platform/pure/corefunctions/"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("nonexistent/but/still/valid/folder"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/folder"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/nonexistent/but/still/valid/folder/"));

        Assertions.assertFalse(CodeStorageTools.isValidFilePath(null));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath(""));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.isValidFilePath("/path/with/special/$#/chars"));
    }

    @Test
    public void testIsValidFolderPath()
    {
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("/platform/pure/corefunctions"));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("/platform/pure/corefunctions/"));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("nonexistent/but/still/valid/folder"));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/folder"));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("nonexistent/but/still/valid/folder/"));
        Assertions.assertTrue(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/folder/"));

        Assertions.assertFalse(CodeStorageTools.isValidFolderPath(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath(WelcomeCodeStorage.WELCOME_FILE_NAME));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.pure"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.csv"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.css"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.html"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.js"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/nonexistent/but/still/valid/file.json"));

        Assertions.assertFalse(CodeStorageTools.isValidFolderPath(null));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath(""));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.isValidFolderPath("/path/with/special/$#/chars"));
    }

    @Test
    public void testIsPureFilePath()
    {
        Assertions.assertTrue(CodeStorageTools.isPureFilePath(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath(WelcomeCodeStorage.WELCOME_FILE_NAME));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.Pure"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.pUre"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.puRe"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.purE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PUre"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PuRe"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PurE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.pURe"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.pUrE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.puRE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PURe"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PUrE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PuRE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.pURE"));
        Assertions.assertTrue(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.PURE"));

        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.csv"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.css"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.html"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.js"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/file.json"));

        Assertions.assertFalse(CodeStorageTools.isPureFilePath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/platform/pure/corefunctions"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/platform/pure/corefunctions/"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/folder"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/nonexistent/but/still/valid/folder/"));

        Assertions.assertFalse(CodeStorageTools.isPureFilePath(null));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath(""));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("/path/with/special/$#/chars"));
        Assertions.assertFalse(CodeStorageTools.isPureFilePath("\\not\\a\\valid\\path\\but\\still\\has\\extension.pure"));
    }

    @Test
    public void testHasPureFileExtension()
    {
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension(WelcomeCodeStorage.WELCOME_FILE_NAME));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.pure"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.Pure"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.pUre"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.puRe"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.purE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PUre"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PuRe"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PurE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.pURe"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.pUrE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.puRE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PURe"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PUrE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PuRE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.pURE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.PURE"));
        Assertions.assertTrue(CodeStorageTools.hasPureFileExtension("\\not\\a\\valid\\path\\but\\still\\has\\extension.pure"));

        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.csv"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.css"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.html"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.js"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/file.json"));

        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/platform/pure/corefunctions"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/platform/pure/corefunctions/"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/folder"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/nonexistent/but/still/valid/folder/"));

        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension(null));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension(""));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.hasPureFileExtension("/path/with/special/$#/chars"));
    }

    @Test
    public void testGetInitialPathElement()
    {
        Assertions.assertNull(CodeStorageTools.getInitialPathElement(null));
        Assertions.assertNull(CodeStorageTools.getInitialPathElement(""));

        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/pure"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/pure"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/pure/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/pure/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/pure/corefunctions"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/pure/corefunctions"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/pure/corefunctions/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/pure/corefunctions/"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertEquals("platform", CodeStorageTools.getInitialPathElement("platform/pure/corefunctions/lang.pure"));
        Assertions.assertEquals(null, CodeStorageTools.getInitialPathElement("welcome.pure"));
        Assertions.assertEquals(null, CodeStorageTools.getInitialPathElement("/welcome.pure"));
        Assertions.assertEquals(null, CodeStorageTools.getInitialPathElement("/welcome.pure/"));
    }

    @Test
    public void testCanonicalizePath()
    {
        Assertions.assertEquals(RepositoryCodeStorage.ROOT_PATH, CodeStorageTools.canonicalizePath(null));
        Assertions.assertEquals(RepositoryCodeStorage.ROOT_PATH, CodeStorageTools.canonicalizePath(""));
        Assertions.assertEquals(RepositoryCodeStorage.ROOT_PATH, CodeStorageTools.canonicalizePath("     \t   \t\t"));

        Assertions.assertEquals("/platform", CodeStorageTools.canonicalizePath("platform"));
        Assertions.assertEquals("/platform", CodeStorageTools.canonicalizePath("/platform"));
        Assertions.assertEquals("/platform/pure/corefunctions/lang.pure", CodeStorageTools.canonicalizePath("platform/pure/corefunctions/lang.pure"));
        Assertions.assertEquals("/platform/pure/corefunctions/lang.pure", CodeStorageTools.canonicalizePath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertEquals("/platform/pure/corefunctions/lang.pure", CodeStorageTools.canonicalizePath("     /platform/pure/corefunctions/lang.pure\t\t"));
    }

    @Test
    public void testIsCanonicalPath()
    {
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath(RepositoryCodeStorage.ROOT_PATH));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath(WelcomeCodeStorage.WELCOME_FILE_PATH));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/platform/pure/corefunctions"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/platform/pure/corefunctions/lang.pure"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.pure"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.csv"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.css"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.html"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.js"));
        Assertions.assertTrue(CodeStorageTools.isCanonicalPath("/nonexistent/but/still/valid.json"));

        Assertions.assertFalse(CodeStorageTools.isCanonicalPath(null));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath(""));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath("not a valid path"));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath("/not a valid path/even though it/almost looks like one"));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath("/path/with/special/$#/chars"));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath("valid/but/not/canonical"));
        Assertions.assertFalse(CodeStorageTools.isCanonicalPath("valid/but/not/canonical/"));
    }

    @Test
    public void testJoinPaths()
    {
        Assertions.assertEquals("", CodeStorageTools.joinPaths());
        Assertions.assertEquals("", CodeStorageTools.joinPaths((String[])null));

        Assertions.assertEquals("platform", CodeStorageTools.joinPaths("platform"));
        Assertions.assertEquals("/platform", CodeStorageTools.joinPaths("/platform"));
        Assertions.assertEquals("platform/", CodeStorageTools.joinPaths("platform/"));
        Assertions.assertEquals("/platform/", CodeStorageTools.joinPaths("/platform/"));

        Assertions.assertEquals("platform/pure/corefunctions", CodeStorageTools.joinPaths("platform", "pure", "corefunctions"));
        Assertions.assertEquals("/platform/pure/corefunctions/", CodeStorageTools.joinPaths("/platform", "pure", "corefunctions/"));
        Assertions.assertEquals("/platform/pure/corefunctions/", CodeStorageTools.joinPaths("/platform", "/pure/", "corefunctions/"));
        Assertions.assertEquals("/platform/pure/corefunctions/", CodeStorageTools.joinPaths("/platform/", "pure/", "/corefunctions/"));

        Assertions.assertEquals("platform/pure/corefunctions/lang.pure", CodeStorageTools.joinPaths("platform", "pure", "corefunctions", "lang.pure"));
        Assertions.assertEquals("/platform/pure/corefunctions/lang.pure", CodeStorageTools.joinPaths("/platform", "pure", "/corefunctions", "lang.pure"));
    }
}
