// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.metadata;

import com.intellij.codeInspection.htmlInspections.HtmlUnknownAttributeInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection;
import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.angular2.Angular2CodeInsightFixtureTestCase;
import org.angular2.inspections.Angular2TemplateInspectionsProvider;
import org.angular2.inspections.AngularAmbiguousComponentTagInspection;
import org.angular2.inspections.AngularUndefinedBindingInspection;
import org.angular2.inspections.AngularUndefinedTagInspection;
import org.angular2.lang.metadata.MetadataJsonFileViewProviderFactory;
import org.angular2.lang.metadata.psi.MetadataFileImpl;
import org.angularjs.AngularTestUtil;

import java.io.File;

public class MetadataTest extends Angular2CodeInsightFixtureTestCase {

  @Override
  protected String getTestDataPath() {
    return AngularTestUtil.getBaseTestDataPath(getClass());
  }

  public void testMetadataJsonFileTypeBinary() {
    VirtualFile vFile = myFixture.copyFileToProject("common.metadata.json");
    myFixture.configureByFiles("package.json", "common.d.ts");
    PsiFile file = myFixture.getPsiManager().findFile(vFile);
    assert file != null;
    assert file.getViewProvider() instanceof MetadataJsonFileViewProviderFactory.MetadataFileViewProvider;
    assert file.getViewProvider().getAllFiles().get(0) instanceof MetadataFileImpl;
  }

  public void testMetadataJsonFileTypeNormal() {
    PsiFile file = myFixture.configureByFiles("common.metadata.json", "package.json")[0];
    assert file instanceof JsonFileImpl;
    assert file.getViewProvider() instanceof SingleRootFileViewProvider;
    assert !(file.getViewProvider() instanceof MetadataJsonFileViewProviderFactory.MetadataFileViewProvider);
    assert file.getViewProvider().getAllFiles().get(0) instanceof JsonFileImpl;
  }

  public void testMetadataStubBuilding() {
    myFixture.configureByFiles("package.json", "ng-zorro-antd.d.ts");
    VirtualFile vFile = myFixture.copyFileToProject("ng-zorro-antd.metadata.json");
    PsiFile file = myFixture.getPsiManager().findFile(vFile);
    assert file instanceof MetadataFileImpl;
    String result = DebugUtil.psiToString(file, false, false);
    UsefulTestCase.assertSameLinesWithFile(new File(getTestDataPath(), "ng-zorro-antd.metadata.psi.txt").toString(), result);
  }

  public void testMetadataStubBuildingWithResolution() {
    AngularTestUtil.configureWithMetadataFiles(myFixture, "ant-design-icons-angular");
    myFixture.configureByFiles("ng-zorro-antd.d.ts", "nz-icon.directive.d.ts", "icon.directive.ts",
                               "nz-col.component.d.ts", "nz-form-control.component.d.ts");
    VirtualFile vFile = myFixture.copyFileToProject("ng-zorro-antd.metadata.json");
    PsiFile file = myFixture.getPsiManager().findFile(vFile);
    assert file instanceof MetadataFileImpl;
    String result = DebugUtil.psiToString(file, false, false);
    UsefulTestCase.assertSameLinesWithFile(new File(getTestDataPath(), "ng-zorro-antd.metadata.resolved.psi.txt").toString(), result);
  }

  public void testJsonFileType() {
    PsiFile file = myFixture.configureByFile("package.json");
    assert file != null;
    assert file.getViewProvider() instanceof SingleRootFileViewProvider;
    assert !(file.getViewProvider() instanceof MetadataJsonFileViewProviderFactory.MetadataFileViewProvider);
    assert file.getViewProvider().getAllFiles().get(0) instanceof JsonFileImpl;
  }

  public void testExtendsObfuscatedName() {
    AngularTestUtil.configureWithMetadataFiles(myFixture, "ng-zorro-antd");
    myFixture.configureByFiles("inherited_properties.html", "nz-col.component.d.ts", "nz-form-control.component.d.ts");
    myFixture.enableInspections(HtmlUnknownAttributeInspection.class,
                                AngularUndefinedBindingInspection.class);
    myFixture.checkHighlighting(true, false, true);
  }

  public void testInterModuleExtends() {
    AngularTestUtil.configureWithMetadataFiles(myFixture, "ng-zorro-antd", "ant-design-icons-angular");
    myFixture.configureByFiles("inter_module_props.html", "nz-icon.directive.d.ts", "icon.directive.ts");
    myFixture.enableInspections(HtmlUnknownAttributeInspection.class,
                                AngularUndefinedBindingInspection.class);
    myFixture.checkHighlighting(true, false, true);
  }

  public void testMetadataWithExportAliases() {
    AngularTestUtil.configureWithMetadataFiles(myFixture, "export.test");
    VirtualFile vFile = myFixture.getTempDirFixture().getFile("export.test.metadata.json");
    PsiFile file = myFixture.getPsiManager().findFile(vFile);
    assert file instanceof MetadataFileImpl;
    String result = DebugUtil.psiToString(file, false, false);
    UsefulTestCase.assertSameLinesWithFile(new File(getTestDataPath(), "export.test.metadata.json.txt").toString(), result);
  }

  public void testMaterialMetadataResolution() {
    myFixture.copyDirectoryToProject("material", ".");
    myFixture.enableInspections(AngularAmbiguousComponentTagInspection.class,
                                AngularUndefinedTagInspection.class);
    myFixture.configureFromTempProjectFile("module.ts");
    myFixture.checkHighlighting();
  }

  public void testMaterialMetadataStubGeneration() {
    myFixture.copyDirectoryToProject("material", ".");
    VirtualFile materialDir = myFixture.getTempDirFixture().getFile("node_modules/@angular/material");
    String pathPrefix = materialDir.getPath();
    PsiDirectory material = myFixture.getPsiManager().findDirectory(materialDir);
    material.acceptChildren(new PsiElementVisitor() {
      @Override
      public void visitFile(PsiFile file) {
        if (file.getName().endsWith(".metadata.json")) {
          String relativeFile = FileUtil.getRelativePath(pathPrefix, file.getVirtualFile().getPath(), '/');
          assert file instanceof MetadataFileImpl : relativeFile;
          String result = DebugUtil.psiToString(file, false, false);
          UsefulTestCase.assertSameLinesWithFile(new File(getTestDataPath(), "material-stubs/" + relativeFile + ".txt").toString(), result);
        }
      }

      @Override
      public void visitDirectory(PsiDirectory dir) {
        String relativeFile = FileUtil.getRelativePath(
          pathPrefix, dir.getVirtualFile().getPath(), '/');
        if (!"typings".equals(relativeFile)) {
          dir.acceptChildren(this);
        }
      }
    });
  }

  public void testIonicMetadataResolution() {
    myFixture.copyDirectoryToProject("ionic", ".");
    myFixture.enableInspections(AngularAmbiguousComponentTagInspection.class,
                                AngularUndefinedTagInspection.class,
                                AngularUndefinedBindingInspection.class,
                                HtmlUnknownTagInspection.class,
                                HtmlUnknownAttributeInspection.class);
    myFixture.configureFromTempProjectFile("tab1.page.html");
    myFixture.checkHighlighting();
    AngularTestUtil.moveToOffsetBySignature("ion-card-<caret>subtitle", myFixture);
    assertEquals("proxies.metadata.json",
                 myFixture.getElementAtCaret().getContainingFile().getName());
  }

  public void testFunctionPropertyMetadata() {
    myFixture.copyDirectoryToProject("function_property", ".");
    myFixture.enableInspections(new Angular2TemplateInspectionsProvider());
    myFixture.configureFromTempProjectFile("template.html");
    myFixture.checkHighlighting();
    assertEquals("my-lib.component.d.ts",
                 myFixture.getElementAtCaret().getContainingFile().getName());
  }
}
