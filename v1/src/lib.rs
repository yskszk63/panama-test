use std::os::raw::c_void;

use jni::sys::{jint, JavaVM as RawJavaVM, JNI_VERSION_1_8};

#[no_mangle]
pub extern "C" fn JNI_OnLoad(_vm: *mut RawJavaVM, _reserved: *mut c_void) -> jint {
    println!("LOADED![{}]", env!("CARGO_PKG_NAME"));
    return JNI_VERSION_1_8;
}

#[no_mangle]
pub extern "C" fn JNI_OnUnload(_vm: *mut RawJavaVM, _reserved: *mut c_void) -> jint {
    println!("UNLOADED![{}]", env!("CARGO_PKG_NAME"));
    return JNI_VERSION_1_8;
}

#[no_mangle]
pub extern "C" fn hello() {
    println!("Hello, world!{}", env!("CARGO_PKG_NAME"))
}
